package at.hannibal2.skyhanni.utils.render.item

import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.BlitRenderState
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.client.renderer.RenderPipelines

internal class SkyHanniRealtimeItemSlot(val slotSize: Int) : AutoCloseable {

    private var texture: GpuTexture? = null
    private var textureView: GpuTextureView? = null
    private var depthTexture: GpuTexture? = null
    private var depthTextureView: GpuTextureView? = null

    init { allocate(slotSize) }

    @Suppress("UnsafeCallOnNullableType")
    private fun allocate(size: Int) {
        val device = RenderSystem.getDevice()
        texture = device.createTexture("SkyHanni realtime item", 12, TextureFormat.RGBA8, size, size, 1, 1)
            //? if < 1.21.11 {
            .also { it.setTextureFilter(FilterMode.NEAREST, false) }
        //?}
        val texture = texture!!
        textureView = device.createTextureView(texture)
        depthTexture = device.createTexture("SkyHanni realtime item depth", 8, TextureFormat.DEPTH32, size, size, 1, 1)
        val depthTexture = depthTexture!!
        depthTextureView = device.createTextureView(depthTexture)
        device.createCommandEncoder().clearColorAndDepthTextures(texture, 0, depthTexture, 1.0)
    }

    fun render(
        context: SkyHanniItemRenderContext,
        state: SkyHanniGuiItemRenderState,
        guiRenderState: GuiRenderState,
        projectionBuffer: CachedOrthoProjectionMatrixBuffer,
    ) {
        val texture = texture ?: return
        val textureView = textureView ?: return
        val depthTextureView = depthTextureView ?: return

        // Clear before rendering
        RenderSystem.getDevice().createCommandEncoder()
            .clearColorAndDepthTextures(texture, 0, depthTexture!!, 1.0)

        val bufferSlice = projectionBuffer.getBuffer(slotSize.toFloat(), slotSize.toFloat())
        RenderSystem.setProjectionMatrix(bufferSlice, ProjectionType.ORTHOGRAPHIC)
        RenderSystem.outputColorTextureOverride = textureView
        RenderSystem.outputDepthTextureOverride = depthTextureView

        state.renderItemToTexture(
            context.bufferSource, context.featureRenderDispatcher,
            centerX = slotSize / 2.0f,
            centerY = slotSize / 2.0f,
            pixelSize = slotSize,
        )

        RenderSystem.outputColorTextureOverride = null
        RenderSystem.outputDepthTextureOverride = null

        // Blit is submitted AFTER the texture override is cleared
        submitBlit(state, guiRenderState)
    }

    private fun submitBlit(
        state: SkyHanniGuiItemRenderState,
        guiRenderState: GuiRenderState,
    ) {
        val textureView = textureView ?: return
        // u/v: full slot occupies [0,1] x [0,1] in the per-item texture
        guiRenderState.submitBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                //? if < 1.21.11 {
                TextureSetup.singleTexture(textureView),
                //?} else
                // TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(),
                state.x0(), state.y0(), state.x1(), state.y1(),
                0f,
                1f,
                1f,
                0f,
                -1,
                state.scissorArea(),
            )
        )
    }

    override fun close() {
        textureView?.close(); textureView = null
        texture?.close(); texture = null
        depthTextureView?.close(); depthTextureView = null
        depthTexture?.close(); depthTexture = null
    }
}
