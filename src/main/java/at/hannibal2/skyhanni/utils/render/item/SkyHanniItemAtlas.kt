package at.hannibal2.skyhanni.utils.render.item

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.util.Mth

internal class SkyHanniItemAtlas : AutoCloseable {

    private var texture: GpuTexture? = null
    private var textureView: GpuTextureView? = null
    private var depthTexture: GpuTexture? = null
    private var depthTextureView: GpuTextureView? = null

    private var sizePixels = 0
    private var slotSize = 0

    private val positions = HashMap<SkyHanniAtlasKey, SkyHanniAtlasPosition>()
    private val animatedFrames = HashMap<SkyHanniAnimatedKey, SkyHanniAtlasPosition>()

    private var cursorX = 0
    private var cursorY = 0

    fun getTextureView(): GpuTextureView? = textureView
    fun getSize() = sizePixels
    fun getSlotSize() = slotSize
    fun getPositions() = positions
    fun getAnimatedFrames() = animatedFrames
    fun getCursorX() = cursorX
    fun getCursorY() = cursorY

    fun advanceCursor() {
        cursorX += slotSize
    }

    fun newRow() {
        cursorX = 0
        cursorY += slotSize
    }

    fun isFull() = cursorY + slotSize > sizePixels ||
        (cursorX + slotSize > sizePixels && cursorY + slotSize * 2 > sizePixels)
    fun isRowFull() = cursorX + slotSize > sizePixels

    fun recordPosition(key: SkyHanniAtlasKey, frameNumber: Int) {
        val u = cursorX.toFloat() / sizePixels.toFloat()
        val v = (sizePixels - cursorY).toFloat() / sizePixels.toFloat()
        positions[key] = SkyHanniAtlasPosition(cursorX, cursorY, u, v, frameNumber)
    }

    fun recordAnimatedPosition(key: SkyHanniAnimatedKey, position: SkyHanniAtlasPosition) {
        animatedFrames[key] = position
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
        val initialSize = calculateSize(16, slotSize)
        allocate(initialSize)
    }

    // Called when the current atlas is full and we need more space.
    // Doubles the atlas size, copies nothing.
    // All positions are invalidated and items re-render next frame.
    fun grow() {
        val newSize = (sizePixels * 2).coerceAtMost(RenderSystem.getDevice().maxTextureSize)
        if (newSize == sizePixels) return // Already at max size, nothing we can do
        positions.clear()
        animatedFrames.clear()
        close()
        cursorX = 0
        cursorY = 0
        allocate(newSize)
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

    fun beginRender() {
        RenderSystem.outputColorTextureOverride = textureView
        RenderSystem.outputDepthTextureOverride = depthTextureView
    }

    fun endRender() {
        RenderSystem.outputColorTextureOverride = null
        RenderSystem.outputDepthTextureOverride = null
    }

    fun clearSlot(x: Int, y: Int, size: Int) {
        RenderSystem.getDevice().createCommandEncoder()
            .clearColorAndDepthTextures(texture!!, 0, depthTexture!!, 1.0, x, sizePixels - y - size, size, size)
    }

    @Suppress("SameParameterValue")
    private fun calculateSize(itemCount: Int, slotSize: Int): Int {
        val side = Mth.smallestSquareSide(itemCount + itemCount / 2)
        val raw = Mth.smallestEncompassingPowerOfTwo(side * slotSize)
        return raw.coerceIn(512, RenderSystem.getDevice().maxTextureSize)
    }
}
