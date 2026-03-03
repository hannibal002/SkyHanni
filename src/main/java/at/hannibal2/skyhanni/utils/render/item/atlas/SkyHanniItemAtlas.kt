package at.hannibal2.skyhanni.utils.render.item.atlas

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.render.PoseStackUtils.mulPose
import at.hannibal2.skyhanni.utils.render.item.SkyHanniGuiItemRenderState
import at.hannibal2.skyhanni.utils.render.item.SkyHanniItemRenderContext
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.BlitRenderState
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.Dumpable
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import java.nio.file.Path

internal class SkyHanniItemAtlas : AbstractTexture(), AutoCloseable, Dumpable {

    companion object {
        private val identifier = Identifier.fromNamespaceAndPath("skyhanni", "item_atlas")
        private const val PADDING = 1
    }

    init {
        Minecraft.getInstance().textureManager.register(identifier, this)
    }

    private var depthTexture: GpuTexture? = null
    private var depthTextureView: GpuTextureView? = null

    private var sizePixels = 0
    private var root: Node? = null
    private var needsGrowing = false
    private val positions = HashMap<SkyHanniAtlasKey, SkyHanniItemAtlasEntry>()

    /**
     * Square-slot BSP bin packer, modelled after FontTexture.Node
     */
    private inner class Node(val x: Int, val y: Int, val width: Int, val height: Int) {
        var left: Node? = null
        var right: Node? = null
        var occupied = false

        fun insert(size: Int): Node? {
            if (left != null || right != null) return left?.insert(size) ?: right?.insert(size)
            if (occupied || size > width || size > height) return null
            if (size == width && size == height) { occupied = true; return this }

            val dw = width - size
            val dh = height - size
            if (dw > dh) {
                left = Node(x, y, size, height)
                right = Node(x + size + PADDING, y, width - size - PADDING, height)
            } else {
                left = Node(x, y, width, size)
                right = Node(x, y + size + PADDING, width, height - size - PADDING)
            }
            return left!!.insert(size)
        }
    }

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

    /**
     * Runs after rendering finishes.
     */
    private fun tryGrow() {
        if (!needsGrowing) return
        val newSize = (sizePixels * 2).coerceAtMost(RenderSystem.getDevice().maxTextureSize)
        if (newSize == sizePixels) {
            ErrorManager.crashInDevEnv("SkyHanni item atlas is full and cannot grow further")
            return
        }
        positions.clear()
        close()
        allocate(newSize)
        needsGrowing = false
    }

    private fun pruneFrames(currentFrame: Int, olderThanLastRenderedFrames: Int = 2) {
        positions.entries.removeIf { (key, pos) ->
            key is SkyHanniAnimatedAtlasKey && pos is SkyHanniAnimatedItemAtlasEntry &&
                currentFrame - pos.lastRenderedFrame > olderThanLastRenderedFrames
        }
    }

    private fun SkyHanniAnimatedAtlasKey.clearPreviousFrame() {
        val prevEntry = positions[this.copy(frameNumber = frameNumber - 1)] ?: return
        clearSlot(prevEntry.x, prevEntry.y, prevEntry.pixelSize)
    }

    private fun recordPosition(key: SkyHanniAtlasKey, slotX: Int, slotY: Int, pixelSize: Int) {
        val u = slotX.toFloat() / sizePixels.toFloat()
        val v = (sizePixels - slotY).toFloat() / sizePixels.toFloat()
        val entry = if (key is SkyHanniAnimatedAtlasKey) {
            SkyHanniAnimatedItemAtlasEntry(slotX, slotY, u, v, pixelSize, key.frameNumber)
        } else {
            SkyHanniItemAtlasEntry(slotX, slotY, u, v, pixelSize)
        }
        positions[key] = entry
    }

    private fun ensureAllocated() {
        if (texture != null) return
        allocate(512.coerceAtMost(RenderSystem.getDevice().maxTextureSize))
    }

    private data class AtlasRenderJob(
        val key: SkyHanniAtlasKey,
        val representative: SkyHanniGuiItemRenderState,
        val node: Node,
        val pixelSize: Int,
    )

    fun SkyHanniItemRenderContext.setupAtlasRendering(
        frameNumber: Int,
        projectionBuffer: CachedOrthoProjectionMatrixBuffer,
    ) {
        pruneFrames(frameNumber)
        if (atlasStates.isEmpty()) return
        ensureAllocated()

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

            val node = root?.insert(neededPixels) ?: run {
                needsGrowing = true
                // Overflow, submitBlitForState will return false and fall back to realtime
                continue
            }

            val representative = states.maxByOrNull { it.adjustedScale }!!
            renderJobs.add(AtlasRenderJob(key, representative, node, neededPixels))
        }

        if (renderJobs.isEmpty()) return

        render(projectionBuffer) {
            for ((key, representative, node, pixelSize) in renderJobs) {
                renderItemToAtlas(representative, node.x, node.y, pixelSize)
                recordPosition(key, node.x, node.y, pixelSize)
            }
            bufferSource.endBatch()
        }
    }

    fun invalidate() {
        positions.clear()
        root = null
        close()
    }

    val usage = GpuTexture.USAGE_RENDER_ATTACHMENT or
        GpuTexture.USAGE_TEXTURE_BINDING or
        GpuTexture.USAGE_COPY_SRC

    private fun allocate(size: Int) {
        sizePixels = size
        root = Node(0, 0, size, size)
        val device = RenderSystem.getDevice()
        texture = device.createTexture("SkyHanni item atlas", usage, TextureFormat.RGBA8, size, size, 1, 1)
            //? if < 1.21.11 {
            .also { it.setTextureFilter(FilterMode.NEAREST, false) }
        //?}
        textureView = device.createTextureView(texture!!)
        depthTexture = device.createTexture("SkyHanni item atlas depth", 8, TextureFormat.DEPTH32, size, size, 1, 1)
        depthTextureView = device.createTextureView(depthTexture!!)
        device.createCommandEncoder().clearColorAndDepthTextures(texture!!, 0, depthTexture!!, 1.0)
    }

    override fun close() {
        textureView?.close()
        textureView = null
        texture?.close()
        texture = null
        depthTextureView?.close()
        depthTextureView = null
        depthTexture?.close()
        depthTexture = null
    }

    private fun render(projectionBuffer: CachedOrthoProjectionMatrixBuffer, block: () -> Unit) {
        val bufferSlice = projectionBuffer.getBuffer(sizePixels.toFloat(), sizePixels.toFloat())
        RenderSystem.setProjectionMatrix(bufferSlice, ProjectionType.ORTHOGRAPHIC)
        RenderSystem.outputColorTextureOverride = textureView
        RenderSystem.outputDepthTextureOverride = depthTextureView
        block()
        RenderSystem.outputColorTextureOverride = null
        RenderSystem.outputDepthTextureOverride = null
        tryGrow()
    }

    private fun SkyHanniItemRenderContext.renderItemToAtlas(
        shState: SkyHanniGuiItemRenderState,
        slotX: Int,
        slotY: Int,
        pixelSize: Int,
    ) {
        val ps = PoseStack()
        ps.translate(slotX.toFloat() + pixelSize / 2.0f, slotY.toFloat() + pixelSize / 2.0f, 0.0f)

        val rotationPadding = 1.0f / 1.42f
        val f = pixelSize.toFloat()
        ps.scale(f, -f, f)
        ps.scale(rotationPadding, rotationPadding, rotationPadding)
        val rotated = ps.mulPose(shState.rotationVector)
        ps.translate(0.0f, 0.03f, 0.125f)

        val gameRenderer = Minecraft.getInstance().gameRenderer
        gameRenderer.lighting.setupFor(
            if (shState.usesBlockLight()) Lighting.Entry.ITEMS_3D else Lighting.Entry.ITEMS_FLAT,
        )
        if (rotated) shState.setAnimated()

        RenderSystem.enableScissorForRenderTypeDraws(
            slotX, sizePixels - slotY - pixelSize, pixelSize, pixelSize,
        )
        shState.submit(ps, featureRenderDispatcher.submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0)
        featureRenderDispatcher.renderAllFeatures()
        bufferSource.endBatch()
        RenderSystem.disableScissorForRenderTypeDraws()
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
        submitBlitRenderState(state, entry.u, entry.v, entry.pixelSize, guiRenderState)
        return true
    }

    private fun submitBlitRenderState(
        shState: SkyHanniGuiItemRenderState,
        u: Float,
        v: Float,
        pixelSize: Int,
        guiRenderState: GuiRenderState,
    ) {
        val textureView = this@SkyHanniItemAtlas.textureView ?: throw IllegalStateException("Atlas not allocated")
        val size = sizePixels.toFloat()
        val slotF = pixelSize.toFloat()
        val u1 = u + slotF / size
        val v1 = v + (-slotF) / size
        guiRenderState.submitBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                //? if < 1.21.11 {
                TextureSetup.singleTexture(textureView),
                //?} else
                // TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                shState.pose(),
                shState.x0(), shState.y0(), shState.x1(), shState.y1(),
                u, u1, v, v1,
                -1,
                shState.scissorArea(),
            )
        )
    }

    private fun clearSlot(x: Int, y: Int, size: Int) {
        val texture = this.texture ?: throw IllegalStateException("Atlas not allocated")
        val depthTexture = this.depthTexture ?: throw IllegalStateException("Atlas not allocated")
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            texture, 0, depthTexture, 1.0,
            x, sizePixels - y - size, size, size,
        )
    }
}
