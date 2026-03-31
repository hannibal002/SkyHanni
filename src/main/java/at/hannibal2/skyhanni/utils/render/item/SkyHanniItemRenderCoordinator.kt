package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniItemAtlas
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.client.renderer.MultiBufferSource.BufferSource
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

internal object SkyHanniItemRenderCoordinator {

    private data class FrameRenderResources(
        val bufferSource: BufferSource,
        val featureRenderDispatcher: FeatureRenderDispatcher,
        val guiScale: Int,
    )
    private var frameResources: FrameRenderResources? = null

    // items actively spinning re-render every frame, same as Mojang's isAnimated path.
    // items that have been stable for this many frames are committed to the atlas.
    private const val SETTLE_FRAMES = 4
    private val projectionBuffer by lazy {
        CachedOrthoProjectionMatrixBuffer("SkyHanni items", -1000.0f, 1000.0f, true)
    }
    private val realtimeSlots = LinkedHashMap<Int, SkyHanniRealtimeItemSlot>()
    private val realtimeSlotLastSeen = HashMap<Int, Int>() // stableId -> frameNumber
    private val settleTracker = HashMap<Int, SettleEntry>() // keyed by stableId, NOT atlasKey
    private val atlas = SkyHanniItemAtlas()
    private var lastEvictFrame = -1

    fun invalidateAtlas() {
        atlas.invalidate()
        settleTracker.clear()
        realtimeSlots.values.forEach { it.close() }
        realtimeSlots.clear()
    }

    fun closeAtlas() {
        atlas.close()
        projectionBuffer.close()
        realtimeSlots.values.forEach { it.close() }
        realtimeSlots.clear()
    }

    // Called once per frame at HEAD of preparePictureInPicture.
    // Renders all items to the atlas. Does NOT submit any blits.
    fun preRenderAtlas(
        pipStates: List<SkyHanniGuiItemRenderState>,
        bufferSource: BufferSource,
        featureRenderDispatcher: FeatureRenderDispatcher,
        frameNumber: Int,
    ) {
        if (pipStates.isEmpty()) return
        handleEviction(frameNumber)

        val guiScale = Minecraft.getInstance().window.guiScale
        frameResources = FrameRenderResources(bufferSource, featureRenderDispatcher, guiScale)
        val atlasStates = ArrayList<SkyHanniGuiItemRenderState>(pipStates.size)

        for (state in pipStates) {
            val settle = settleTracker.getOrPut(state.stableId) {
                SettleEntry(state.rotationVector, state.adjustedScale, 0)
            }
            val stable = settle.rotationVec == state.rotationVector &&
                abs(settle.lastScale - state.adjustedScale) < 0.01f
            if (stable) settle.framesStable++
            else {
                settle.rotationVec = state.rotationVector
                settle.lastScale = state.adjustedScale
                settle.framesStable = 0
            }
            if (settle.framesStable >= SETTLE_FRAMES || (!state.isAnimated() && stable))
                atlasStates.add(state)
        }

        if (atlasStates.isEmpty()) return

        val renderContext = SkyHanniItemRenderContext(
            atlasStates, bufferSource, featureRenderDispatcher, frameNumber, guiScale,
        )

        with(atlas) { renderContext.setupAtlasRendering(frameNumber, projectionBuffer) }
    }

    // Called per-item at TAIL of preparePictureInPictureState.
    // Atlas is already populated; just look up entry and submit blit, or fall back to realtime.
    fun submitBlit(
        state: SkyHanniGuiItemRenderState,
        guiRenderState: GuiRenderState,
        frameNumber: Int,
    ) {
        val resources = frameResources ?: return
        val isSettled = settleTracker[state.stableId]?.let {
            val smallChange = !state.isAnimated() && abs(it.lastScale - state.adjustedScale) < 0.01f
            it.framesStable >= SETTLE_FRAMES || smallChange
        } ?: false

        if (isSettled) {
            val blitSubmitted = with(atlas) { submitBlitForState(state, guiRenderState, frameNumber) }
            if (blitSubmitted) return
            // Atlas miss (overflow or not yet allocated) — fall through to realtime
        }

        realtimeSlotLastSeen[state.stableId] = frameNumber
        val slotSize = (16 * resources.guiScale * state.adjustedScale).toInt()
        val existing = realtimeSlots[state.stableId]
        val slot = if (existing != null && existing.slotSize == slotSize) existing
        else {
            existing?.close()
            SkyHanniRealtimeItemSlot(slotSize).also { realtimeSlots[state.stableId] = it }
        }
        val renderContext = SkyHanniItemRenderContext(
            atlasStates = emptyList(),
            resources.bufferSource,
            resources.featureRenderDispatcher,
            frameNumber,
            resources.guiScale,
        )
        slot.render(renderContext, state, guiRenderState, projectionBuffer)
    }

    private fun handleEviction(frameNumber: Int) {
        if (frameNumber == lastEvictFrame) return
        lastEvictFrame = frameNumber
        realtimeSlots.entries.removeIf { (id, slot) ->
            val stale = realtimeSlotLastSeen.getOrDefault(id, -1) < frameNumber - 1
            if (stale) {
                slot.close()
                realtimeSlotLastSeen.remove(id)
            }
            stale
        }
    }

    private data class SettleEntry(var rotationVec: Vec3, var lastScale: Float, var framesStable: Int)
}
