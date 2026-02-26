package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniAtlasKey
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
        val animatedStates = ArrayList<SkyHanniGuiItemRenderState>(pipStates.size)
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
            if (settle.framesStable >= SETTLE_FRAMES || !state.isAnimated()) staticFallbackStates.add(state)
            else animatedStates.add(state)
        }

        SkyHanniItemRenderContext(
            animatedStates, staticFallbackStates, guiRenderState,
            bufferSource, featureRenderDispatcher,
            frameNumber, guiScale,
        ).setupRendering()
    }

    private fun SkyHanniItemRenderContext.setupRendering() {
        if (atlasStates.isNotEmpty()) with(atlas) {
            setupAtlasRendering(frameNumber, projectionBuffer)
        }
        if (fallbackStates.isNotEmpty()) {
            val fallbackRenderer = SkyHanniItemRenderer(bufferSource)
            fallbackStates.forEach { state -> fallbackRenderer.prepare(state, guiRenderState, guiScale) }
        }
    }

}
