package at.hannibal2.skyhanni.utils.render.item

import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import com.mojang.blaze3d.textures.FilterMode
import kotlin.math.roundToInt

//? if >= 26.1
import org.joml.Matrix4f

internal class SkyHanniRealtimeItemSlot(val slotSize: Int) : SkyHanniAbstractItemTexture() {

    init { allocate(slotSize) }

    private fun allocate(size: Int) {
        allocateTextures(
            size, "SkyHanni realtime item", "SkyHanni realtime item depth",
            GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_COPY_DST,
        )
    }

    fun render(
        context: SkyHanniItemRenderContext,
        state: SkyHanniGuiItemRenderState,
        guiRenderState: GuiRenderState,
        projectionBuffer: ProjectionMatrixBuffer,
    ) {
        val texture = texture ?: return
        val textureView = textureView ?: return
        val depthTexture = depthTexture ?: return
        val depthTextureView = depthTextureView ?: return

        // Clear before rendering
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(texture, 0, depthTexture, 1.0)

        val size = slotSize.toFloat()
        //~ if < 26.1 'Matrix4f().setOrtho(0f, size, size, 0f, -1000f, 1000f)' -> 'size, size'
        val bufferSlice = projectionBuffer.getBuffer(Matrix4f().setOrtho(0f, size, size, 0f, -1000f, 1000f))

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
        guiRenderState.addBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(),
                state.x0(), state.y0(), state.x1(), state.y1(),
                0f,
                1f,
                1f,
                0f,
                ((state.alpha * 255).roundToInt() shl 24) or 0x00FFFFFF,
                state.scissorArea(),
            )
        )
    }
}
