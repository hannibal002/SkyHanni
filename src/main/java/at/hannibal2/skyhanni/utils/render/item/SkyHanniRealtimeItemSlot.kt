package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.render.PoseStackUtils.mulPose
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

internal class SkyHanniRealtimeItemSlot(private val slotSize: Int) : AutoCloseable {

    private var texture: GpuTexture? = null
    private var textureView: GpuTextureView? = null
    private var depthTexture: GpuTexture? = null
    private var depthTextureView: GpuTextureView? = null

    init { allocate(slotSize) }

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

        with(context) {
            val ps = PoseStack()
            ps.translate(slotSize / 2.0f, slotSize / 2.0f, 0.0f)
            val f = slotSize.toFloat()
            ps.scale(f, -f, f)
            ps.scale(1.0f / 1.42f, 1.0f / 1.42f, 1.0f / 1.42f)
            val rotated = ps.mulPose(state.rotationVector)
            ps.translate(0.0f, 0.03f, 0.125f)

            Minecraft.getInstance().gameRenderer.lighting.setupFor(
                if (state.usesBlockLight()) Lighting.Entry.ITEMS_3D else Lighting.Entry.ITEMS_FLAT
            )
            if (rotated) state.setAnimated()

            state.submit(ps, featureRenderDispatcher.submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0)
            featureRenderDispatcher.renderAllFeatures()
            bufferSource.endBatch()
        }

        RenderSystem.outputColorTextureOverride = null
        RenderSystem.outputDepthTextureOverride = null

        // Blit is submitted AFTER the texture override is cleared
        submitBlit(context, state)
    }

    private fun submitBlit(context: SkyHanniItemRenderContext, state: SkyHanniGuiItemRenderState) {
        val textureView = textureView ?: return
        // u/v: full slot occupies [0,1] x [0,1] in the per-item texture
        context.guiRenderState.submitBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                //? if < 1.21.11 {
                TextureSetup.singleTexture(textureView),
                //?} else
                // TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(),
                state.x0(), state.y0(), state.x1(), state.y1(),
                0f, 1f,   // u0, u1
                1f, 0f,   // v0, v1  (flipped: rendered bottom-up)
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
