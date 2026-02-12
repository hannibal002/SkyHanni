package at.hannibal2.skyhanni.utils.render.item

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

internal class SkyHanniItemRenderCoordinator(bufferSource: BufferSource) {

    companion object {
        // items actively spinning re-render every frame, same as mojang's isAnimated path.
        // items that have been stable for this many frames are committed to the atlas.
        private const val SETTLE_FRAMES = 4
        private val projectionBuffer = CachedOrthoProjectionMatrixBuffer("SkyHanni items", -1000.0f, 1000.0f, true)
        private val settleTracker = HashMap<SkyHanniAnimatedKey, SettleEntry>()
        internal val atlas = SkyHanniItemAtlas()
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
    }

    private val fallbackRenderer = SkyHanniItemRenderer(bufferSource)

    private data class SettleEntry(var rotVec: Vec3, var framesStable: Int)

    fun prepare(
        states: List<SkyHanniGuiItemRenderState>,
        guiRenderState: GuiRenderState,
        bufferSource: BufferSource,
        featureRenderDispatcher: FeatureRenderDispatcher,
        frameNumber: Int,
    ) {
        if (states.isEmpty()) return

        val guiScale = Minecraft.getInstance().window.guiScale
        val staticStates = ArrayList<SkyHanniGuiItemRenderState>(states.size)
        val animatedStates = ArrayList<SkyHanniGuiItemRenderState>(states.size)

        for (state in states) {
            val tracking = trackingStateOf(state) ?: continue
            val baseKey = SkyHanniAnimatedKey(tracking.modelIdentity, state.scale, guiScale)

            if (state.rotVec == Vec3.ZERO) {
                staticStates.add(state)
                continue
            }

            val settle = settleTracker.getOrPut(baseKey) { SettleEntry(state.rotVec, 0) }
            if (settle.rotVec == state.rotVec) {
                settle.framesStable++
            } else {
                settle.rotVec = state.rotVec
                settle.framesStable = 0
            }

            if (settle.framesStable >= SETTLE_FRAMES) staticStates.add(state)
            else animatedStates.add(state)
        }

        val maxScale = states.maxOf { it.scale }
        atlas.ensureCapacity(guiScale, maxScale)

        RenderSystem.setProjectionMatrix(
            projectionBuffer.getBuffer(atlas.getSize().toFloat(), atlas.getSize().toFloat()),
            ProjectionType.ORTHOGRAPHIC,
        )
        atlas.beginRender()

        val fallbackStates = mutableListOf<SkyHanniGuiItemRenderState>()
        val context = SkyHanniItemRenderContext(
            states,
            guiRenderState,
            bufferSource,
            featureRenderDispatcher,
            frameNumber,
            guiScale,
            fallbackStates
        )
        context.renderStaticItems()
        context.renderAnimatedItems()

        bufferSource.endBatch()
        atlas.endRender()
        if (atlasNeedsGrow && atlas.getSize() < RenderSystem.getDevice().maxTextureSize) {
            atlas.grow()
            atlasNeedsGrow = false
        }
        fallbackStates.forEach { state -> fallbackRenderer.prepare(state, guiRenderState, guiScale)}
    }

    private fun SkyHanniItemRenderContext.renderStaticItems() = states.forEach { state ->
        val tracking = trackingStateOf(state) ?: return@forEach
        val key = SkyHanniAtlasKey(tracking.modelIdentity, state.rotVec, state.scale, guiScale)
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
        submitBlit(state, atlas.getPositions()[key]!!.u, atlas.getPositions()[key]!!.v)
        atlas.advanceCursor()
    }

    private fun SkyHanniItemRenderContext.renderAnimatedItems() = states.forEach { state ->
        val tracking = trackingStateOf(state) ?: return@forEach
        val animKey = SkyHanniAnimatedKey(tracking.modelIdentity, state.scale, guiScale)
        val existing = atlas.getAnimatedFrames()[animKey]

        // reuse last frame's slot position for this animated item, clearing and re-rendering into it
        // this mirrors exactly what mojang does for isAnimated items in prepareItemElements()
        val slotX: Int
        val slotY: Int

        if (existing != null && existing.lastRenderedFrame != frameNumber) {
            slotX = existing.x
            slotY = existing.y
            atlas.clearSlot(slotX, slotY, atlas.getSlotSize())
        } else {
            if (atlas.isRowFull()) atlas.newRow()
            if (atlas.isFull()) {
                if (atlas.getSize() < RenderSystem.getDevice().maxTextureSize) atlas.grow()
                fallbackStates.add(state)
                return@forEach
            }
            slotX = atlas.getCursorX()
            slotY = atlas.getCursorY()
            atlas.advanceCursor()
        }

        renderItemToAtlas(state, tracking, slotX, slotY, atlas.getSlotSize())

        val u = slotX.toFloat() / atlas.getSize().toFloat()
        val v = (atlas.getSize() - slotY).toFloat() / atlas.getSize().toFloat()

        val position = SkyHanniAtlasPosition(slotX, slotY, u, v, frameNumber)
        atlas.recordAnimatedPosition(animKey, position)
        submitBlit(state, u, v)
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
        val f = slotSize.toFloat()
        ps.scale(f, -f, f)
        ps.scale(1.0f, -1.0f, -1.0f)

        val rotated = ps.mulPose(state.rotVec)
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
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
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
