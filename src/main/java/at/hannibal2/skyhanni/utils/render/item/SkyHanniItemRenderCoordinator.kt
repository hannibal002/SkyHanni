package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.render.PoseStackUtils.mulPose
import at.hannibal2.skyhanni.utils.render.SkyHanniGuiItemRenderState
import at.hannibal2.skyhanni.utils.render.SkyHanniItemRenderer
import com.mojang.blaze3d.ProjectionType
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
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.phys.Vec3

internal object SkyHanniItemRenderCoordinator {

    // items actively spinning re-render every frame, same as mojang's isAnimated path.
    // items that have been stable for this many frames are committed to the atlas.
    private const val SETTLE_FRAMES = 4
    private val projectionBuffer = CachedOrthoProjectionMatrixBuffer("SkyHanni items", -1000.0f, 1000.0f, true)
    private val settleTracker = HashMap<SkyHanniAnimatedKey, SettleEntry>()
    private val atlas = SkyHanniItemAtlas()
    private val log = LorenzLogger("render/items")

    @JvmStatic
    private var atlasNeedsGrow = false

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
        states: List<SkyHanniGuiItemRenderState>,
        guiRenderState: GuiRenderState,
        bufferSource: BufferSource,
        featureRenderDispatcher: FeatureRenderDispatcher,
        frameNumber: Int,
    ) {
        if (states.isEmpty()) return

        val guiScale = Minecraft.getInstance().window.guiScale
        val animatedStates = ArrayList<SkyHanniGuiItemRenderState>(states.size)
        val staticFallbackStates = ArrayList<SkyHanniGuiItemRenderState>(states.size)

        for (state in states) {
            val tracking = trackingStateOf(state) ?: continue
            val baseKey = SkyHanniAnimatedKey(tracking.modelIdentity, state.scale, guiScale, state.stableId)

            // Track rotation stability
            val settle = settleTracker.getOrPut(baseKey) { SettleEntry(state.rotationVec, 0) }
            if (settle.rotationVec == state.rotationVec) settle.framesStable++
            else {
                settle.rotationVec = state.rotationVec
                settle.framesStable = 0
            }

            // Items that haven't moved in 4+ frames use fallback (direct rendering)
            /*if (settle.framesStable >= SETTLE_FRAMES) staticFallbackStates.add(state)
            else animatedStates.add(state)*/

            // Please?
            animatedStates.add(state)
        }

        // Only set up atlas if we have animated items
        trySetupAtlasRendering(
            animatedStates,
            guiRenderState,
            bufferSource,
            featureRenderDispatcher,
            frameNumber,
            guiScale
        )

        val fallbackRenderer = SkyHanniItemRenderer(bufferSource)
        staticFallbackStates.forEach { state ->
            fallbackRenderer.prepare(state, guiRenderState, guiScale)
        }
    }

    private fun trySetupAtlasRendering(
        animatedStates: List<SkyHanniGuiItemRenderState>,
        guiRenderState: GuiRenderState,
        bufferSource: BufferSource,
        featureRenderDispatcher: FeatureRenderDispatcher,
        frameNumber: Int,
        guiScale: Int,
    ) {
        atlas.getAnimatedFrames().entries.removeIf { (_, pos) -> frameNumber - pos.lastRenderedFrame > 2 }
        if (animatedStates.isEmpty()) return
        val maxScale = animatedStates.maxOf { it.scale }
        atlas.ensureCapacity(guiScale, maxScale)

        RenderSystem.setProjectionMatrix(
            projectionBuffer.getBuffer(atlas.getSize().toFloat(), atlas.getSize().toFloat()),
            ProjectionType.ORTHOGRAPHIC,
        )
        atlas.beginRender()

        val fallbackStates = mutableListOf<SkyHanniGuiItemRenderState>()
        val context = SkyHanniItemRenderContext(
            animatedStates,
            guiRenderState,
            bufferSource,
            featureRenderDispatcher,
            frameNumber,
            guiScale,
            fallbackStates
        )
        context.renderAnimatedItems()

        bufferSource.endBatch()
        atlas.endRender()
        if (atlasNeedsGrow && atlas.getSize() < RenderSystem.getDevice().maxTextureSize) {
            atlas.grow()
            atlasNeedsGrow = false
        }
        val fallbackRenderer = SkyHanniItemRenderer(bufferSource)
        fallbackStates.forEach { state -> fallbackRenderer.prepare(state, guiRenderState, guiScale)}
    }

    private fun SkyHanniItemRenderContext.renderStaticItems() = states.forEach { state ->
        val tracking = trackingStateOf(state) ?: return@forEach
        val key = SkyHanniAtlasKey(tracking.modelIdentity, state.rotationVec, state.scale, guiScale)
        val existing = atlas.getPositions()[key]

        if (existing != null) return@forEach submitBlit(state, existing.u, existing.v)
        if (atlas.isRowFull()) atlas.newRow()
        if (atlas.isFull()) {
            atlasNeedsGrow = true
            fallbackStates.add(state)
            return@forEach
        }

        renderItemToAtlas(state, tracking, atlas.getCursorX(), atlas.getCursorY(), atlas.getSlotSize())

        atlas.recordPosition(key, frameNumber)
        val positions = atlas.getPositions()[key] ?: throw Error("Recorded position not found")
        submitBlit(state, positions.u, positions.v)
        atlas.advanceCursor()
    }

    private fun SkyHanniItemRenderContext.renderAnimatedItems() {
        states.forEach { state ->
            val tracking = trackingStateOf(state) ?: return@forEach
            val animKey = SkyHanniAnimatedKey(tracking.modelIdentity, state.scale, guiScale, state.stableId)
            val existing = atlas.getAnimatedFrames()[animKey]

            val slotX: Int
            val slotY: Int

            if (existing != null && existing.lastRenderedFrame != frameNumber) {
                // Reuse existing slot - _dont_ advance cursor
                slotX = existing.x
                slotY = existing.y
                atlas.clearSlot(slotX, slotY, atlas.getSlotSize())
            } else if (existing == null) {
                // First time seeing this animated item - allocate new slot
                if (atlas.isRowFull()) atlas.newRow()
                if (atlas.isFull()) {
                    if (atlas.getSize() < RenderSystem.getDevice().maxTextureSize) atlas.grow()
                    fallbackStates.add(state)
                    return@forEach
                }
                slotX = atlas.getCursorX()
                slotY = atlas.getCursorY()
                atlas.advanceCursor()
            } else return@forEach

            renderItemToAtlas(state, tracking, slotX, slotY, atlas.getSlotSize())

            val u = slotX.toFloat() / atlas.getSize().toFloat()
            val v = (atlas.getSize() - slotY).toFloat() / atlas.getSize().toFloat()

            val position = SkyHanniAtlasPosition(slotX, slotY, u, v, frameNumber)
            atlas.recordAnimatedPosition(animKey, position)
            submitBlit(state, u, v)
        }
    }

    private fun SkyHanniItemRenderContext.renderItemToAtlas(
        state: SkyHanniGuiItemRenderState,
        tracking: TrackingItemStackRenderState,
        slotX: Int,
        slotY: Int,
        slotSize: Int,
    ) {
        val ps = PoseStack()
        ps.translate(slotX.toFloat() + slotSize / 2.0f, slotY.toFloat() + slotSize / 2.0f, 0.0f)
        ps.translate(state.translationVec)

        val rotationPadding = 1.0f / 1.42f  // sqrt 2 factor for safety
        val f = slotSize.toFloat()
        ps.scale(f, -f, f)

        ps.scale(rotationPadding, rotationPadding, rotationPadding)
        val rotated = ps.mulPose(state.rotationVec)
        ps.translate(0.0f, 0.03f, 0.125f)

        val gameRenderer = Minecraft.getInstance().gameRenderer
        gameRenderer.lighting.setupFor(
            if (tracking.usesBlockLight()) Lighting.Entry.ITEMS_3D
            else Lighting.Entry.ITEMS_FLAT,
        )
        if (rotated) tracking.setAnimated()

        RenderSystem.enableScissorForRenderTypeDraws(
            slotX,
            atlas.getSize() - slotY - slotSize,
            slotSize,
            slotSize,
        )
        tracking.submit(ps, featureRenderDispatcher.submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0)
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
                TextureSetup.singleTexture(textureView),
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

    private fun trackingStateOf(state: SkyHanniGuiItemRenderState): TrackingItemStackRenderState? =
        state.guiItemRenderState().itemStackRenderState()
}
