package at.hannibal2.skyhanni.utils.render.item.atlas

import at.hannibal2.skyhanni.utils.render.PoseStackUtils.mulPose
import at.hannibal2.skyhanni.utils.render.item.SkyHanniGuiItemRenderState
import at.hannibal2.skyhanni.utils.render.item.SkyHanniItemRenderContext
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.BlitRenderState
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.OverlayTexture
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

    private fun pruneFrames(currentFrame: Int, olderThanLastRenderedFrames: Int = 2) {
        animatedFrames.entries.removeIf { (_, pos) -> currentFrame - pos.lastRenderedFrame > olderThanLastRenderedFrames }
    }

    private fun SkyHanniAnimatedAtlasKey.clearPreviousFrame() {
        val previousKey = this.copy(frameNumber = frameNumber - 1)
        clearSlot(
            animatedFrames[previousKey]?.x ?: return,
            animatedFrames[previousKey]?.y ?: return,
            slotSize
        )
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

    private fun recordPosition(key: SkyHanniAtlasKey, slotX: Int, slotY: Int): SkyHanniItemAtlasEntry {
        val u = slotX.toFloat() / sizePixels.toFloat()
        val v = (sizePixels - slotY).toFloat() / sizePixels.toFloat()
        return if (key is SkyHanniAnimatedAtlasKey) {
            SkyHanniAnimatedItemAtlasEntry(slotX, slotY, u, v, key.frameNumber).also { animatedFrames[key] = it }
        } else {
            SkyHanniItemAtlasEntry(slotX, slotY, u, v).also { positions[key] = it }
        }
    }

    fun SkyHanniItemRenderContext.setupAtlasRendering(
        frameNumber: Int,
        projectionBuffer: CachedOrthoProjectionMatrixBuffer,
    ) {
        pruneFrames(frameNumber)
        if (atlasStates.isEmpty()) return
        ensureCapacity(guiScale, atlasStates.maxOf { it.scale })

        render(projectionBuffer) {
            atlasStates.forEach { state ->
                val stateKey = state.getAtlasKey(guiScale) ?: return@forEach
                val (slotX, slotY) = stateKey.getCursorPosition {
                    fallbackStates.add(state)
                } ?: return@forEach

                renderItemToAtlas(state, slotX, slotY)
                val atlasEntry = recordPosition(stateKey, slotX, slotY)
                submitBlitRenderState(state, atlasEntry.u, atlasEntry.v)
            }
            bufferSource.endBatch()
        }
    }

    private fun SkyHanniAtlasKey.getCursorPosition(onAtlasMiss: () -> Unit): Pair<Int, Int>? {
        val existing = if (this is SkyHanniAnimatedAtlasKey) animatedFrames[this]
        else positions[this]
        return if (existing != null) {
            if (this is SkyHanniAnimatedAtlasKey) clearPreviousFrame()
            existing.x to existing.y
        } else if (checkRowFull() || checkFull()) {
            onAtlasMiss()
            null
        } else (cursorX to cursorY).also {
            cursorX += slotSize
        }
    }

    // Called once per frame. Only creates or grows the atlas; never shrinks or resets it.
    // slotSize is fixed per guiScale + maxScale combination.
    //  If it changes we must invalidate externally.
    private fun ensureCapacity(guiScale: Int, maxScale: Float) {
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
            //? if < 1.21.11 {
            .also { it.setTextureFilter(FilterMode.NEAREST, false) }
        //?}
        val texture = texture ?: throw IllegalStateException("Failed to create atlas texture")
        textureView = device.createTextureView(texture)
        depthTexture = device.createTexture("SkyHanni item atlas depth", 8, TextureFormat.DEPTH32, size, size, 1, 1)
        val depthTexture = depthTexture ?: throw IllegalStateException("Failed to create atlas depth texture")
        depthTextureView = device.createTextureView(depthTexture)
        device.createCommandEncoder().clearColorAndDepthTextures(texture, 0, depthTexture, 1.0)
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
    ) {
        val ps = PoseStack()
        ps.translate(slotX.toFloat() + slotSize / 2.0f, slotY.toFloat() + slotSize / 2.0f, 0.0f)

        val rotationPadding = 1.0f / 1.42f
        val f = slotSize.toFloat()
        ps.scale(f, -f, f)

        ps.scale(rotationPadding, rotationPadding, rotationPadding)
        val rotated = ps.mulPose(shState.rotationVec)
        ps.translate(0.0f, 0.03f, 0.125f)

        val gameRenderer = Minecraft.getInstance().gameRenderer
        gameRenderer.lighting.setupFor(
            if (shState.usesBlockLight()) Lighting.Entry.ITEMS_3D
            else Lighting.Entry.ITEMS_FLAT,
        )
        if (rotated) shState.setAnimated()

        RenderSystem.enableScissorForRenderTypeDraws(
            slotX,
            sizePixels - slotY - slotSize,
            slotSize,
            slotSize,
        )
        shState.submit(ps, featureRenderDispatcher.submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0)
        featureRenderDispatcher.renderAllFeatures()
        bufferSource.endBatch()
        RenderSystem.disableScissorForRenderTypeDraws()
    }

    private fun SkyHanniItemRenderContext.submitBlitRenderState(
        shState: SkyHanniGuiItemRenderState,
        u: Float,
        v: Float,
    ) {
        val textureView = this@SkyHanniItemAtlas.textureView ?: throw IllegalStateException("Atlas not allocated")
        val size = sizePixels.toFloat()
        val slotSize = slotSize.toFloat()
        val u1 = u + slotSize / size
        val v1 = v + (-slotSize) / size
        guiRenderState.submitBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                //? if < 1.21.11 {
                TextureSetup.singleTexture(textureView),
                //?} else
                // TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                shState.pose(),
                shState.x0(),
                shState.y0(),
                shState.x1(),
                shState.y1(),
                u,
                u1,
                v,
                v1,
                -1,
                shState.scissorArea(),
            )
        )
    }

    private fun clearSlot(x: Int, y: Int, size: Int) {
        val texture = this.texture ?: throw IllegalStateException("Atlas not allocated")
        val depthTexture = this.depthTexture ?: throw IllegalStateException("Atlas not allocated")
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            texture,
            0,
            depthTexture,
            1.0,
            x,
            sizePixels - y - size,
            size,
            size
        )
    }
}
