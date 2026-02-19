package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.render.PoseStackUtils.mulPose
import at.hannibal2.skyhanni.utils.render.SkyHanniGuiAnimatedItemRenderState
import at.hannibal2.skyhanni.utils.render.SkyHanniGuiItemRenderState
import at.hannibal2.skyhanni.utils.render.SkyHanniItemRenderer
import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniAtlasKey
import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniItemAtlas
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.BlitRenderState
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.client.renderer.MultiBufferSource.BufferSource
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.phys.Vec3

internal object SkyHanniItemRenderCoordinator {

    // items actively spinning re-render every frame, same as mojang's isAnimated path.
    // items that have been stable for this many frames are committed to the atlas.
    private const val SETTLE_FRAMES = 4
    private val projectionBuffer = CachedOrthoProjectionMatrixBuffer("SkyHanni items", -1000.0f, 1000.0f, true)
    private val settleTracker = HashMap<SkyHanniAtlasKey, SettleEntry>()
    private val atlas = SkyHanniItemAtlas()

    fun invalidateAtlas() {
        atlas.invalidate()
        settleTracker.clear()
    }

    fun closeAtlas() {
        atlas.close()
        projectionBuffer.close()
    }

    private data class SettleEntry(var rotationVec: Vec3, var framesStable: Int)

    fun prepare(
        pipStates: List<SkyHanniGuiItemRenderState>,
        guiRenderState: GuiRenderState,
        bufferSource: BufferSource,
        featureRenderDispatcher: FeatureRenderDispatcher,
        frameNumber: Int,
    ) {
        if (pipStates.isEmpty()) return

        val guiScale = Minecraft.getInstance().window.guiScale
        val animatedStates = ArrayList<SkyHanniGuiAnimatedItemRenderState>(pipStates.size)
        val staticFallbackStates = ArrayList<SkyHanniGuiItemRenderState>(pipStates.size)

        for (state in pipStates) {
            val stateKey = state.getAtlasKey(guiScale) ?: continue

            // Track rotation stability
            val settle = settleTracker.getOrPut(stateKey) { SettleEntry(state.rotationVec, 0) }
            if (settle.rotationVec == state.rotationVec) settle.framesStable++
            else {
                settle.rotationVec = state.rotationVec
                settle.framesStable = 0
            }

            // Items that haven't moved in 4+ frames (or are static) use fallback (direct rendering)
            if (settle.framesStable >= SETTLE_FRAMES || state !is SkyHanniGuiAnimatedItemRenderState) staticFallbackStates.add(state)
            else animatedStates.add(state)
        }

        // Only set up atlas if we have animated items
        if (animatedStates.isNotEmpty()) SkyHanniItemRenderContext(
            animatedStates, staticFallbackStates, guiRenderState,
            bufferSource, featureRenderDispatcher,
            frameNumber, guiScale,
        ).trySetupAtlasRendering()

        val fallbackRenderer = SkyHanniItemRenderer(bufferSource)
        staticFallbackStates.forEach { state ->
            fallbackRenderer.prepare(state, guiRenderState, guiScale)
        }
    }

    private fun SkyHanniItemRenderContext.trySetupAtlasRendering() = with(atlas) {
        pruneFrames(frameNumber)
        if (atlasStates.isEmpty()) return
        ensureCapacity(guiScale, atlasStates.maxOf { it.scale })

        render(projectionBuffer) {
            atlasStates.forEach { state ->
                val stateKey = state.getAtlasKey(guiScale) ?: return@forEach
                val (slotX, slotY) = stateKey.getCursorPosition {
                    fallbackStates.add(state)
                } ?: return@forEach

                renderItemToAtlas(state, slotX, slotY, atlas.getSlotSize())
                val atlasEntry = atlas.recordPosition(stateKey, slotX, slotY)
                    ?: throw Error("Entry not found")
                submitBlit(state, atlasEntry.u, atlasEntry.v)
            }
            bufferSource.endBatch()
        }

        val fallbackRenderer = SkyHanniItemRenderer(bufferSource)
        fallbackStates.forEach { state -> fallbackRenderer.prepare(state, guiRenderState, guiScale)}
    }

    private fun SkyHanniItemRenderContext.renderItemToAtlas(
        state: SkyHanniGuiItemRenderState,
        slotX: Int,
        slotY: Int,
        slotSize: Int,
    ) {
        val ps = PoseStack()
        ps.translate(slotX.toFloat() + slotSize / 2.0f, slotY.toFloat() + slotSize / 2.0f, 0.0f)

        val rotationPadding = 1.0f / 1.42f
        val f = slotSize.toFloat()
        ps.scale(f, -f, f)

        ps.scale(rotationPadding, rotationPadding, rotationPadding)
        val rotated = ps.mulPose(state.rotationVec)
        ps.translate(0.0f, 0.03f, 0.125f)

        val gameRenderer = Minecraft.getInstance().gameRenderer
        gameRenderer.lighting.setupFor(
            if (state.usesBlockLight()) Lighting.Entry.ITEMS_3D
            else Lighting.Entry.ITEMS_FLAT,
        )
        if (rotated) state.setAnimated()

        RenderSystem.enableScissorForRenderTypeDraws(
            slotX,
            atlas.getSize() - slotY - slotSize,
            slotSize,
            slotSize,
        )
        state.submit(ps, featureRenderDispatcher.submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0)
        featureRenderDispatcher.renderAllFeatures()
        bufferSource.endBatch()
        RenderSystem.disableScissorForRenderTypeDraws()
    }

    private fun SkyHanniItemRenderContext.submitBlit(
        state: SkyHanniGuiItemRenderState,
        u: Float,
        v: Float,
    ) {
        val size = atlas.getSize().toFloat()
        val slotSize = atlas.getSlotSize().toFloat()
        val u1 = u + slotSize / size
        val v1 = v + (-slotSize) / size
        val textureView = atlas.getTextureView() ?: throw Error("TextureView")
        guiRenderState.submitBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                //? if < 1.21.11
                TextureSetup.singleTexture(textureView),
                //?} else {
                // TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST))
                state.pose(),
                state.x0(),
                state.y0(),
                state.x1(),
                state.y1(),
                u,
                u1,
                v,
                v1,
                -1,
                state.scissorArea(),
            )
        )
    }
}
