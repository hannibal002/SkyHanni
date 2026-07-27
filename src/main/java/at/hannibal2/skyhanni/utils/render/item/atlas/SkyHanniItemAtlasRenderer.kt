package at.hannibal2.skyhanni.utils.render.item.atlas

import at.hannibal2.skyhanni.utils.render.item.SkyHanniAbstractItemTexture.Companion.CLEAR_DEPTH
import at.hannibal2.skyhanni.utils.render.item.SkyHanniGuiItemRenderState
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.TextureSetup
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

//? if >= 26.1 {
import net.minecraft.client.renderer.Projection
//?}

internal class SkyHanniItemAtlasRenderer(
    private val sizePixels: Int,
    private val textureView: GpuTextureView,
    private val depthTextureView: GpuTextureView,
    private val texture: GpuTexture,
    private val depthTexture: GpuTexture,
) {

    //? if >= 26.1 {
    private val projection = Projection()
    //?}

    fun render(
        projectionBuffer: ProjectionMatrixBuffer,
        block: () -> Unit,
    ) {
        val size = sizePixels.toFloat()
        //? if >= 26.1 {
        projection.setupOrtho(-1000f, 1000f, size, size, true)
        val bufferSlice = projectionBuffer.getBuffer(projection)
        //?} else {
        /*val bufferSlice = projectionBuffer.getBuffer(size, size)
        *///?}
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
        //? if >= 26.2 {
        submitNodeStorage: SubmitNodeStorage,
        //?} else {
        /*bufferSource: MultiBufferSource.BufferSource,
        *///?}
        featureRenderDispatcher: FeatureRenderDispatcher,
    ) {
        RenderSystem.enableScissorForRenderTypeDraws(
            slotX, sizePixels - slotY - pixelSize, pixelSize, pixelSize,
        )
        shState.renderItemToTexture(
            //? if >= 26.2 {
            submitNodeStorage,
            //?} else {
            /*bufferSource,
            *///?}
            featureRenderDispatcher,
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
                TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
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
            texture,
            GuiRenderer.CLEAR_COLOR,
            depthTexture,
            CLEAR_DEPTH,
            x, sizePixels - y - size, size, size,
        )
    }
}
