package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.VectorUtils.isEqualTo
import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniItemAtlas
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

//? if >= 26.2 {
import net.minecraft.client.renderer.SubmitNodeStorage
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource
*///?}

@SkyHanniModule
internal object SkyHanniItemRenderCoordinator {
    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Item Atlas")
        event.addIrrelevant {
            atlas.atlasDebugInfo().onEach(::add)
            add("Realtime slot pools: ${slotPools.size} sizes, ${slotPools.values.sumOf { it.slots.size }} slots")
            add("Settle tracker entries: ${settleTracker.size}")
        }
    }

    //? if >= 26.2
    private val submitNodeStorage = SubmitNodeStorage()

    private data class FrameRenderResources(
        //~ if < 26.2 'submitNodeStorage: SubmitNodeStorage' -> 'bufferSource: MultiBufferSource.BufferSource'
        val submitNodeStorage: SubmitNodeStorage,
        val featureRenderDispatcher: FeatureRenderDispatcher,
        val guiScale: Int,
    )
    private var frameResources: FrameRenderResources? = null

    // Items actively spinning re-render every frame, same as Mojang's isAnimated path.
    // Items that have been stable for this many frames are committed to the atlas.
    private const val SETTLE_FRAMES = 4

    // Pooled slots idle for this many frames are closed, and stale settle entries dropped
    private const val IDLE_EVICT_FRAMES = 100
    private val projectionBuffer by lazy { ProjectionMatrixBuffer("SkyHanni items") }

    // Realtime slots are cleared and re-rendered every frame, so they carry no item identity.
    // Pooling them by size lets frames reuse textures even when callers lose their stableId.
    private class RealtimeSlotPool {
        val slots = ArrayList<SkyHanniRealtimeItemSlot>()
        var cursor = 0
    }
    private val slotPools = HashMap<Int, RealtimeSlotPool>() // slotSize -> pool
    private var poolFrame = -1
    private val settleTracker = HashMap<Int, SettleEntry>() // keyed by stableId, NOT atlasKey
    private val atlas by lazy { SkyHanniItemAtlas() }
    private var lastEvictFrame = -1

    fun invalidateAtlas() {
        atlas.invalidate()
        settleTracker.clear()
        closeRealtimeSlots()
    }

    fun closeAtlas() {
        atlas.close()
        projectionBuffer.close()
        closeRealtimeSlots()
    }

    private fun closeRealtimeSlots() {
        slotPools.values.forEach { pool -> pool.slots.forEach { it.close() } }
        slotPools.clear()
    }

    // Called once per frame at HEAD of preparePictureInPicture.
    // Renders all items to the atlas. Does NOT submit any blits.
    fun preRenderAtlas(
        pipStates: List<SkyHanniGuiItemRenderState>,
        //? if < 26.2
        //bufferSource: MultiBufferSource.BufferSource,
        featureRenderDispatcher: FeatureRenderDispatcher,
        frameNumber: Int,
    ) {
        if (pipStates.isEmpty()) {
            frameResources = null
            return
        }

        val guiScale = Minecraft.getInstance().window.guiScale
        //~ if < 26.2 'submitNodeStorage' -> 'bufferSource'
        frameResources = FrameRenderResources(submitNodeStorage, featureRenderDispatcher, guiScale)
        val atlasStates = ArrayList<SkyHanniGuiItemRenderState>(pipStates.size)

        for (state in pipStates) {
            val settle = settleTracker.getOrPut(state.stableId) {
                SettleEntry(state.rotationVector, state.adjustedScale, 0, frameNumber)
            }
            settle.lastSeenFrame = frameNumber
            val stable = settle.rotationVec.isEqualTo(state.rotationVector) &&
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
            //~ if < 26.2 'submitNodeStorage' -> 'bufferSource'
            atlasStates, submitNodeStorage, featureRenderDispatcher, frameNumber, guiScale,
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
            // Atlas miss (overflow or not yet allocated) - fall through to realtime
        }

        val slotSize = (16 * resources.guiScale * state.adjustedScale).toInt()
        val slot = acquireRealtimeSlot(slotSize, frameNumber)
        val renderContext = SkyHanniItemRenderContext(
            atlasStates = emptyList(),
            //~ if < 26.2 'submitNodeStorage' -> 'bufferSource'
            resources.submitNodeStorage,
            resources.featureRenderDispatcher,
            frameNumber,
            resources.guiScale,
        )
        slot.render(renderContext, state, guiRenderState, projectionBuffer)
    }

    private fun acquireRealtimeSlot(slotSize: Int, frameNumber: Int): SkyHanniRealtimeItemSlot {
        if (poolFrame != frameNumber) {
            poolFrame = frameNumber
            slotPools.values.forEach { it.cursor = 0 }
        }
        val pool = slotPools.getOrPut(slotSize) { RealtimeSlotPool() }
        val slot = pool.slots.getOrNull(pool.cursor) ?: SkyHanniRealtimeItemSlot(slotSize).also(pool.slots::add)
        pool.cursor++
        slot.lastUsedFrame = frameNumber
        return slot
    }

    @JvmStatic
    fun evictUnusedRealtimeSlots(frameNumber: Int) {
        if (frameNumber == lastEvictFrame) return
        lastEvictFrame = frameNumber
        slotPools.values.removeIf { pool ->
            // Slots are acquired front-to-back, so the stalest slot is always last
            while (pool.slots.isNotEmpty() && frameNumber - pool.slots.last().lastUsedFrame > IDLE_EVICT_FRAMES) {
                pool.slots.removeLast().close()
            }
            pool.slots.isEmpty()
        }
        if (frameNumber % IDLE_EVICT_FRAMES == 0) {
            settleTracker.values.removeIf { frameNumber - it.lastSeenFrame > IDLE_EVICT_FRAMES }
        }
    }

    private data class SettleEntry(
        var rotationVec: Vec3,
        var lastScale: Float,
        var framesStable: Int,
        var lastSeenFrame: Int,
    )
}
