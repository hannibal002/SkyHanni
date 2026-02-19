package at.hannibal2.skyhanni.utils.render.item.atlas

import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.util.Mth

internal class SkyHanniItemAtlas : AutoCloseable {

    private var texture: GpuTexture? = null
    private var textureView: GpuTextureView? = null
    private var depthTexture: GpuTexture? = null
    private var depthTextureView: GpuTextureView? = null

    private var sizePixels = 0
    private var slotSize = 0

    private val positions = HashMap<SkyHanniAtlasKey, SkyHanniItemAtlasEntry>()
    private val animatedFrames = HashMap<SkyHanniAnimatedAtlasKey, SkyHanniAnimatedItemAtlasEntry>()

    private var cursorX = 0
    private var cursorY = 0

    private var needsGrowing = false

    /**
     * Runs after rendering finishes.
     */
    private fun tryGrow() {
        if (!needsGrowing) return

        val newSize = (sizePixels * 2).coerceAtMost(RenderSystem.getDevice().maxTextureSize)
        if (newSize == sizePixels) return // Already at max size, nothing we can do

        positions.clear()
        animatedFrames.clear()
        close()
        cursorX = 0
        cursorY = 0
        allocate(newSize)
        needsGrowing = false
    }

    fun pruneFrames(currentFrame: Int, olderThanLastRenderedFrames: Int = 2) {
        animatedFrames.entries.removeIf { (_, pos) -> currentFrame - pos.lastRenderedFrame > olderThanLastRenderedFrames }
    }

    fun getTextureView(): GpuTextureView? = textureView
    fun getSize() = sizePixels
    fun getSlotSize() = slotSize
    fun getPositions() = positions
    private fun getCursorX() = cursorX
    fun getCursorY() = cursorY

    private fun SkyHanniAtlasKey.getExistingAtlasEntry(): SkyHanniItemAtlasEntry? =
        positions[this]

    private fun SkyHanniAnimatedAtlasKey.getExistingAnimatedAtlasEntry(): SkyHanniAnimatedItemAtlasEntry? =
        animatedFrames[this]

    private fun SkyHanniAnimatedAtlasKey.clearPreviousFrame() {
        val previousKey = this.copy(frameNumber = frameNumber - 1)
        clearSlot(
            animatedFrames[previousKey]?.x ?: return,
            animatedFrames[previousKey]?.y ?: return,
            slotSize
        )
    }

    private fun advanceCursor() {
        cursorX += slotSize
    }

    private fun newRow() {
        cursorX = 0
        cursorY += slotSize
    }

    private fun checkFull(): Boolean = isFull().also { if (it) needsGrowing = true }
    private fun checkRowFull(): Boolean = isRowFull().also { if (it) newRow() }

    private fun isFull() = cursorY + slotSize > sizePixels ||
        (cursorX + slotSize > sizePixels && cursorY + slotSize * 2 > sizePixels)
    private fun isRowFull() = cursorX + slotSize > sizePixels

    fun recordPosition(key: SkyHanniAtlasKey, slotX: Int, slotY: Int): SkyHanniItemAtlasEntry? {
        val u = slotX.toFloat() / sizePixels.toFloat()
        val v = (sizePixels - slotY).toFloat() / sizePixels.toFloat()
        if (key is SkyHanniAnimatedAtlasKey) {
            animatedFrames[key] = SkyHanniAnimatedItemAtlasEntry(slotX, slotY, u, v, key.frameNumber)
            return animatedFrames[key]
        } else {
            positions[key] = SkyHanniItemAtlasEntry(slotX, slotY, u, v)
            return positions[key]
        }
    }

    fun SkyHanniAtlasKey.getCursorPosition(onAtlasMiss: () -> Unit): Pair<Int, Int>? {
        val existing = if (this is SkyHanniAnimatedAtlasKey) getExistingAnimatedAtlasEntry()
        else getExistingAtlasEntry()
        return if (existing != null) {
            if (this is SkyHanniAnimatedAtlasKey) clearPreviousFrame()
            existing.x to existing.y
        } else if (checkRowFull() || checkFull()) {
            onAtlasMiss()
            null
        } else (cursorX to cursorY).also {
            advanceCursor()
        }
    }

    // Called once per frame. Only creates or grows the atlas; never shrinks or resets it.
    // slotSize is fixed per guiScale + maxScale combination.
    //  If it changes we must invalidate externally.
    fun ensureCapacity(guiScale: Int, maxScale: Float) {
        val newSlotSize = (16 * guiScale * maxScale).toInt()

        // If slotSize changes the entire atlas is invalid since all cached positions used the old size.
        if (newSlotSize != slotSize) {
            invalidate()
            slotSize = newSlotSize
        }
        if (texture != null) return

        // Start with a modest size - the atlas grows if we run out of space.
        val side = Mth.smallestSquareSide(24)
        val raw = Mth.smallestEncompassingPowerOfTwo(side * slotSize)
        val initialSize = raw.coerceIn(512, RenderSystem.getDevice().maxTextureSize)
        allocate(initialSize)
    }

    fun invalidate() {
        positions.clear()
        animatedFrames.clear()
        cursorX = 0
        cursorY = 0
        slotSize = 0
        close()
    }

    private fun allocate(size: Int) {
        sizePixels = size
        val device = RenderSystem.getDevice()
        texture = device.createTexture("SkyHanni item atlas", 12, TextureFormat.RGBA8, size, size, 1, 1)
            .also { it.setTextureFilter(FilterMode.NEAREST, false) }
        textureView = device.createTextureView(texture!!)
        depthTexture = device.createTexture("SkyHanni item atlas depth", 8, TextureFormat.DEPTH32, size, size, 1, 1)
        depthTextureView = device.createTextureView(depthTexture!!)
        device.createCommandEncoder().clearColorAndDepthTextures(texture!!, 0, depthTexture!!, 1.0)
    }

    override fun close() {
        textureView?.close(); textureView = null
        texture?.close(); texture = null
        depthTextureView?.close(); depthTextureView = null
        depthTexture?.close(); depthTexture = null
    }

    fun render(projectionBuffer: CachedOrthoProjectionMatrixBuffer, block: () -> Unit) {
        val bufferSlice = projectionBuffer.getBuffer(sizePixels.toFloat(), sizePixels.toFloat())
        RenderSystem.setProjectionMatrix(bufferSlice, ProjectionType.ORTHOGRAPHIC)
        RenderSystem.outputColorTextureOverride = textureView
        RenderSystem.outputDepthTextureOverride = depthTextureView

        block()

        RenderSystem.outputColorTextureOverride = null
        RenderSystem.outputDepthTextureOverride = null

        tryGrow()
    }

    private fun clearSlot(x: Int, y: Int, size: Int) {
        RenderSystem.getDevice().createCommandEncoder()
            .clearColorAndDepthTextures(texture!!, 0, depthTexture!!, 1.0, x, sizePixels - y - size, size, size)
    }
}
