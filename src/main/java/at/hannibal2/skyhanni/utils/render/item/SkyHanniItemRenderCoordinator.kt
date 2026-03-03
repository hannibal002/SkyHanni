package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniItemAtlas
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.client.renderer.MultiBufferSource.BufferSource
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.world.phys.Vec3

internal object SkyHanniItemRenderCoordinator {

    // items actively spinning re-render every frame, same as mojang's isAnimated path.
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

    private data class SettleEntry(var rotationVec: Vec3, var framesStable: Int)

    fun prepare(
        pipStates: List<SkyHanniGuiItemRenderState>,
        guiRenderState: GuiRenderState,
        bufferSource: BufferSource,
        featureRenderDispatcher: FeatureRenderDispatcher,
        frameNumber: Int,
    ) {
        if (pipStates.isEmpty()) return

        // Evict stale realtime slots once per frame, not once per item
        if (frameNumber != lastEvictFrame) {
            lastEvictFrame = frameNumber
            realtimeSlots.entries.removeIf { (id, slot) ->
                val stale = realtimeSlotLastSeen.getOrDefault(id, -1) < frameNumber - 1
                if (stale) slot.close()
                stale
            }
        }

        val guiScale = Minecraft.getInstance().window.guiScale
        val atlasStates = ArrayList<SkyHanniGuiItemRenderState>(pipStates.size)
        val realtimeStates = ArrayList<SkyHanniGuiItemRenderState>(pipStates.size)

        for (state in pipStates) {
            // Key settle tracker by stableId so spinning items don't create new entries every frame
            val settle = settleTracker.getOrPut(state.stableId) { SettleEntry(state.rotationVector, 0) }
            if (settle.rotationVec == state.rotationVector) settle.framesStable++
            else {
                settle.rotationVec = state.rotationVector
                settle.framesStable = 0
            }

            if (settle.framesStable >= SETTLE_FRAMES || !state.isAnimated()) atlasStates.add(state)
            else realtimeStates.add(state)
        }

        val renderContext = SkyHanniItemRenderContext(
            atlasStates, realtimeStates, guiRenderState,
            bufferSource, featureRenderDispatcher, frameNumber, guiScale,
        )

        if (atlasStates.isNotEmpty()) with(atlas) {
            renderContext.setupAtlasRendering(frameNumber, projectionBuffer)
        }

        realtimeStates.forEach { state ->
            realtimeSlotLastSeen[state.stableId] = frameNumber
            val slotSize = (16 * guiScale * state.adjustedScale).toInt()
            val slot = realtimeSlots.getOrPut(state.stableId) {
                SkyHanniRealtimeItemSlot(slotSize)
            }
            slot.render(renderContext, state, projectionBuffer)
        }
    }
}
