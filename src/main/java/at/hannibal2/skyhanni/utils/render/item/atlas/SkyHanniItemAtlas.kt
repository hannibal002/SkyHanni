package at.hannibal2.skyhanni.utils.render.item.atlas

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.render.item.SkyHanniAbstractItemTexture
import at.hannibal2.skyhanni.utils.render.item.SkyHanniGuiItemRenderState
import at.hannibal2.skyhanni.utils.render.item.SkyHanniItemRenderContext
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.client.renderer.texture.Dumpable
import net.minecraft.resources.Identifier
import java.nio.file.Path

internal class SkyHanniItemAtlas : SkyHanniAbstractItemTexture(), Dumpable {

    companion object {
        private val identifier = Identifier.fromNamespaceAndPath("skyhanni", "item_atlas")
    }

    private var sizePixels = 0
    private var packer: SkyHanniAtlasBinPacker? = null
    private var renderer: SkyHanniItemAtlasRenderer? = null
    private val positions = HashMap<SkyHanniAtlasKey, SkyHanniItemAtlasEntry>()

    private val usage = GpuTexture.USAGE_RENDER_ATTACHMENT or
        GpuTexture.USAGE_TEXTURE_BINDING or
        GpuTexture.USAGE_COPY_SRC

    override fun dumpContents(id: Identifier, path: Path) {
        val texture = this.texture ?: return
        val string = id.toDebugFileName()
        try {
            TextureUtil.writeAsPNG(path, string, texture, 0) { i -> i }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Failed to dump atlas texture",
                "id" to id.toString(),
                "path" to path.toString()
            )
        }
    }

    private fun ensureAllocated() {
        if (texture != null) return
        Minecraft.getInstance().textureManager.register(identifier, this)
        allocate(512.coerceAtMost(RenderSystem.getDevice().maxTextureSize))
    }

    private fun allocate(size: Int) {
        sizePixels = size
        allocateTextures(size, "SkyHanni item atlas", "SkyHanni item atlas depth", usage)
        packer = SkyHanniAtlasBinPacker(size)
        @Suppress("UnsafeCallOnNullableType")
        renderer = SkyHanniItemAtlasRenderer(size, textureView!!, depthTextureView!!, texture!!, depthTexture!!)
    }

    private fun pruneFrames(currentFrame: Int, olderThanLastRenderedFrames: Int = 2) {
        positions.entries.removeIf { (key, pos) ->
            key is SkyHanniAnimatedAtlasKey && pos is SkyHanniAnimatedItemAtlasEntry &&
                currentFrame - pos.lastRenderedFrame > olderThanLastRenderedFrames
        }
    }

    private fun SkyHanniAnimatedAtlasKey.clearPreviousFrame() {
        val prevEntry = positions[this.copy(frameNumber = frameNumber - 1)] ?: return
        renderer?.clearSlot(prevEntry.x, prevEntry.y, prevEntry.pixelSize)
    }

    private fun recordPosition(key: SkyHanniAtlasKey, slotX: Int, slotY: Int, pixelSize: Int) {
        val u = slotX.toFloat() / sizePixels.toFloat()
        val v = (sizePixels - slotY).toFloat() / sizePixels.toFloat()
        positions[key] = if (key is SkyHanniAnimatedAtlasKey) {
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
        projectionBuffer: CachedOrthoProjectionMatrixBuffer,
    ) {
        pruneFrames(frameNumber)
        if (atlasStates.isEmpty()) return
        ensureAllocated()
        val renderer = renderer ?: return
        val packer = packer ?: return

        val groups = LinkedHashMap<SkyHanniAtlasKey, MutableList<SkyHanniGuiItemRenderState>>()
        for (state in atlasStates) groups.getOrPut(state.atlasKey) { mutableListOf() }.add(state)

        val renderJobs = mutableListOf<AtlasRenderJob>()

        for ((key, states) in groups) {
            val neededPixels = states.maxOf { (16 * guiScale * it.adjustedScale).toInt() }
            val existing = positions[key]

            if (existing != null && existing.pixelSize >= neededPixels) {
                if (key is SkyHanniAnimatedAtlasKey) key.clearPreviousFrame()
                // Cache hit, no render job needed, blit submitted later per-item
                continue
            }

            // Overflow, submitBlitForState will return false and fall back to realtime
            val node = packer.insert(neededPixels) ?: continue

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
        val entry = positions[state.atlasKey] ?: return false
        if (entry is SkyHanniAnimatedItemAtlasEntry) {
            positions[state.atlasKey] = SkyHanniAnimatedItemAtlasEntry(
                entry.x, entry.y, entry.u, entry.v, entry.pixelSize, frameNumber
            )
        }
        renderer?.submitBlitForState(state, guiRenderState, entry)
        return true
    }

    fun invalidate() {
        positions.clear()
        close()
    }

    override fun close() {
        super.close()
        packer = null
        renderer = null
    }
}
