package at.hannibal2.skyhanni.utils.render.item.atlas

import at.hannibal2.skyhanni.utils.render.atlas.SkyHanniAbstractAtlas
import at.hannibal2.skyhanni.utils.render.atlas.SkyHanniAtlasBinPacker
import at.hannibal2.skyhanni.utils.render.item.SkyHanniGuiItemRenderState
import at.hannibal2.skyhanni.utils.render.item.SkyHanniItemRenderContext
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.resources.Identifier

//? if >= 26.1
import org.joml.Matrix4f

internal class SkyHanniItemAtlas : SkyHanniAbstractAtlas<SkyHanniAtlasKey, SkyHanniItemAtlasEntry>() {

    override val identifier: Identifier by lazy {
        Identifier.fromNamespaceAndPath("skyhanni", "item_atlas")
    }

    override val colorLabel = "SkyHanni item atlas"
    override val depthLabel = "SkyHanni item atlas depth"

    private var renderer: SkyHanniItemAtlasRenderer? = null

    override fun onAllocated() {
        @Suppress("UnsafeCallOnNullableType")
        renderer = SkyHanniItemAtlasRenderer(sizePixels, textureView!!, depthTextureView!!, texture!!, depthTexture!!)
    }

    private fun pruneFrames(currentFrame: Int, olderThanLastRenderedFrames: Int = 2) {
        entries.entries.removeIf { (key, pos) ->
            key is SkyHanniAnimatedAtlasKey && pos is SkyHanniAnimatedItemAtlasEntry &&
                currentFrame - pos.lastRenderedFrame > olderThanLastRenderedFrames
        }
    }

    private fun SkyHanniAnimatedAtlasKey.clearPreviousFrame() {
        val prevEntry = entries[this.copy(frameNumber = frameNumber - 1)] ?: return
        renderer?.clearSlot(prevEntry.x, prevEntry.y, prevEntry.pixelSize)
    }

    private fun recordPosition(key: SkyHanniAtlasKey, slotX: Int, slotY: Int, pixelSize: Int) {
        val (u, v) = uvForSlot(slotX, slotY)
        entries[key] = if (key is SkyHanniAnimatedAtlasKey) {
            SkyHanniAnimatedItemAtlasEntry(slotX, slotY, u, v, pixelSize, key.frameNumber)
        } else {
            SkyHanniItemAtlasEntry(slotX, slotY, u, v, pixelSize)
        }
    }

    private data class AtlasRenderJob(
        val key: SkyHanniAtlasKey,
        val representative: SkyHanniGuiItemRenderState,
        val node: SkyHanniAtlasBinPacker.PackedNode,
        val pixelSize: Int,
    )

    fun SkyHanniItemRenderContext.setupAtlasRendering(
        frameNumber: Int,
        projectionBuffer: ProjectionMatrixBuffer,
    ) {
        pruneFrames(frameNumber)
        if (atlasStates.isEmpty()) return
        ensureAllocated()
        val renderer = renderer ?: return

        val groups = LinkedHashMap<SkyHanniAtlasKey, MutableList<SkyHanniGuiItemRenderState>>()
        for (state in atlasStates) groups.getOrPut(state.atlasKey) { mutableListOf() }.add(state)

        val renderJobs = mutableListOf<AtlasRenderJob>()

        for ((key, states) in groups) {
            val neededPixels = states.maxOf { (16 * guiScale * it.adjustedScale).toInt() }
            val existing = entries[key]

            if (existing != null && existing.pixelSize >= neededPixels) {
                if (key is SkyHanniAnimatedAtlasKey) key.clearPreviousFrame()
                // Cache hit, no render job needed, blit submitted later per-item
                continue
            }

            // Overflow, submitBlitForState will return false and fall back to realtime
            val node = tryInsert(neededPixels) ?: continue

            val representative = states.maxByOrNull { it.adjustedScale }!!
            renderJobs.add(AtlasRenderJob(key, representative, node, neededPixels))
        }

        if (renderJobs.isEmpty()) return

        renderer.render(projectionBuffer) {
            for ((key, representative, node, pixelSize) in renderJobs) {
                renderer.renderItemToAtlas(representative, node.x, node.y, pixelSize, bufferSource, featureRenderDispatcher)
                recordPosition(key, node.x, node.y, pixelSize)
            }
            bufferSource.endBatch()
        }
    }

    // Returns false if no atlas entry exists (overflow/not yet settled), caller falls back to realtime
    fun submitBlitForState(
        state: SkyHanniGuiItemRenderState,
        guiRenderState: GuiRenderState,
        frameNumber: Int,
    ): Boolean {
        val entry = entries[state.atlasKey] ?: return false
        if (entry is SkyHanniAnimatedItemAtlasEntry) {
            entries[state.atlasKey] = SkyHanniAnimatedItemAtlasEntry(
                entry.x, entry.y, entry.u, entry.v, entry.pixelSize, frameNumber
            )
        }
        renderer?.submitBlitForState(state, guiRenderState, entry)
        return true
    }

    override fun close() {
        super.close()
        renderer = null
    }
}
