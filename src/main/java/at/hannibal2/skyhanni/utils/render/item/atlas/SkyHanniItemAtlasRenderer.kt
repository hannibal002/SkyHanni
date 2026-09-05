package at.hannibal2.skyhanni.utils.render.item.atlas

import at.hannibal2.skyhanni.utils.compat.RenderCompat
import at.hannibal2.skyhanni.utils.render.item.SkyHanniGuiItemRenderState
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.Projection
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import kotlin.math.roundToInt

//? if >= 26.2 {
import net.minecraft.client.renderer.SubmitNodeStorage
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource
*///?}

internal class SkyHanniItemAtlasRenderer(
    private val sizePixels: Int,
    private val colorTextureView: GpuTextureView,
    // TODO 26.3
    @Suppress("UnusedPrivateProperty")
    private val depthTextureView: GpuTextureView,
    private val colorTexture: GpuTexture,
    private val depthTexture: GpuTexture,
) {
    fun render(
        projectionBuffer: ProjectionMatrixBuffer,
        block: () -> Unit,
    ) {
        val size = sizePixels.toFloat()
        val bufferSlice = projectionBuffer.getBuffer(Projection().apply { setupOrtho(-1000f, 1000f, size, size, true) })
        RenderSystem.setProjectionMatrix(bufferSlice, ProjectionType.ORTHOGRAPHIC)

        block()
    }

    fun renderItemToAtlas(
        shState: SkyHanniGuiItemRenderState,
        slotX: Int,
        slotY: Int,
        pixelSize: Int,
        //~ if < 26.2 'submitNodeStorage: SubmitNodeStorage' -> 'bufferSource: MultiBufferSource.BufferSource'
        submitNodeStorage: SubmitNodeStorage,
        featureRenderDispatcher: FeatureRenderDispatcher,
    ) {
        RenderSystem.enableScissorForRenderTypeDraws(
            slotX, sizePixels - slotY - pixelSize, pixelSize, pixelSize,
        )
        shState.renderItemToTexture(
            //~ if < 26.2 'submitNodeStorage' -> 'bufferSource'
            submitNodeStorage,
            featureRenderDispatcher,
            colorTextureView,
            depthTextureView,
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
        guiRenderState.addBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(colorTextureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                shState.pose(),
                shState.x0(), shState.y0(), shState.x1(), shState.y1(),
                u, u1, v, v1,
                ((shState.alpha * 255).roundToInt() shl 24) or 0x00FFFFFF,
                shState.scissorArea(),
            )
        )
    }

    fun clearSlot(x: Int, y: Int, size: Int) {
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            colorTexture,
            GuiRenderer.CLEAR_COLOR,
            depthTexture,
            RenderCompat.CLEAR_DEPTH,
            x,
            sizePixels - y - size,
            size,
            size,
            // TODO 26.3
            //? if >= 26.3
            TODO("mipLevel"),
        )
    }
}
