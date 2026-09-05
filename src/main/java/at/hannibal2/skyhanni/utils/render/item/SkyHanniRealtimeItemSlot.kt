package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.compat.RenderCompat
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.Projection
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import kotlin.math.roundToInt

internal class SkyHanniRealtimeItemSlot(val slotSize: Int) : SkyHanniAbstractItemTexture() {
    // Frame the pool last handed this slot out; used for idle eviction
    var lastUsedFrame = -1

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
        val colorTexture = texture ?: return
        val colorTextureView = textureView ?: return
        val depthTexture = depthTexture ?: return
        val depthTextureView = depthTextureView ?: return

        // Clear before rendering
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            colorTexture,
            GuiRenderer.CLEAR_COLOR,
            depthTexture,
            RenderCompat.CLEAR_DEPTH,
        )

        val size = slotSize.toFloat()
        val bufferSlice = projectionBuffer.getBuffer(Projection().apply { setupOrtho(-1000f, 1000f, size, size, true) })

        RenderSystem.setProjectionMatrix(bufferSlice, ProjectionType.ORTHOGRAPHIC)

        state.renderItemToTexture(
            //~ if < 26.2 'submitNodeStorage' -> 'bufferSource'
            context.submitNodeStorage,
            context.featureRenderDispatcher,
            colorTextureView,
            depthTextureView,
            centerX = slotSize / 2.0f,
            centerY = slotSize / 2.0f,
            pixelSize = slotSize,
        )

        // Blit is submitted AFTER the texture override is cleared
        submitBlit(state, guiRenderState)
    }

    private fun submitBlit(
        state: SkyHanniGuiItemRenderState,
        guiRenderState: GuiRenderState,
    ) {
        val colorTextureView = textureView ?: return
        // u/v: full slot occupies [0,1] x [0,1] in the per-item texture
        guiRenderState.addBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(colorTextureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(),
                state.x0(),
                state.y0(),
                state.x1(),
                state.y1(),
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
