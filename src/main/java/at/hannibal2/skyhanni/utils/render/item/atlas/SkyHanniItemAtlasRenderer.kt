package at.hannibal2.skyhanni.utils.render.item.atlas

import at.hannibal2.skyhanni.utils.render.item.SkyHanniGuiItemRenderState
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.BlitRenderState
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
//? if > 1.21.10
// import com.mojang.blaze3d.textures.FilterMode

internal class SkyHanniItemAtlasRenderer(
    private val sizePixels: Int,
    private val textureView: GpuTextureView,
    private val depthTextureView: GpuTextureView,
    private val texture: GpuTexture,
    private val depthTexture: GpuTexture,
) {

    fun render(projectionBuffer: CachedOrthoProjectionMatrixBuffer, block: () -> Unit) {
        val bufferSlice = projectionBuffer.getBuffer(sizePixels.toFloat(), sizePixels.toFloat())
        RenderSystem.setProjectionMatrix(bufferSlice, ProjectionType.ORTHOGRAPHIC)
        RenderSystem.outputColorTextureOverride = textureView
        RenderSystem.outputDepthTextureOverride = depthTextureView
        block()
        RenderSystem.outputColorTextureOverride = null
        RenderSystem.outputDepthTextureOverride = null
    }

    fun renderItemToAtlas(
        shState: SkyHanniGuiItemRenderState,
        slotX: Int,
        slotY: Int,
        pixelSize: Int,
        bufferSource: MultiBufferSource.BufferSource,
        featureRenderDispatcher: FeatureRenderDispatcher,
    ) {
        RenderSystem.enableScissorForRenderTypeDraws(
            slotX, sizePixels - slotY - pixelSize, pixelSize, pixelSize,
        )
        shState.renderItemToTexture(
            bufferSource, featureRenderDispatcher,
            centerX = slotX.toFloat() + pixelSize / 2.0f,
            centerY = slotY.toFloat() + pixelSize / 2.0f,
            pixelSize = pixelSize,
        )
        RenderSystem.disableScissorForRenderTypeDraws()
    }

    fun submitBlitForState(
        state: SkyHanniGuiItemRenderState,
        guiRenderState: GuiRenderState,
        entry: SkyHanniItemAtlasEntry,
    ) = submitBlitRenderState(state, entry.u, entry.v, entry.pixelSize, guiRenderState)

    private fun submitBlitRenderState(
        shState: SkyHanniGuiItemRenderState,
        u: Float,
        v: Float,
        pixelSize: Int,
        guiRenderState: GuiRenderState,
    ) {
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

    fun clearSlot(x: Int, y: Int, size: Int) {
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            texture, 0, depthTexture, 1.0,
            x, sizePixels - y - size, size, size,
        )
    }
}
