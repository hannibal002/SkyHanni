package at.hannibal2.skyhanni.utils.render.atlas

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f

internal class SkyHanniRoundedShapeAtlas : SkyHanniAbstractAtlas<SkyHanniRoundedShapeAtlasKey, SkyHanniRoundedShapeAtlasEntry>() {

    override val identifier: Identifier by lazy {
        Identifier.fromNamespaceAndPath("skyhanni", "rounded_shape_atlas")
    }

    companion object {
        private const val PADDING = 5
    }

    override val colorLabel = "SkyHanni rounded shape atlas"
    override val depthLabel = "SkyHanni rounded shape atlas depth"

    /**
     * Submits a blit for a shape already in the atlas. Returns false if the shape has not been
     * pre-rendered yet (atlas miss), in which case the caller should submit a deferred state instead.
     *
     * @param key the atlas key identifying the shape to blit
     * @param guiRenderState the current frame's GUI render state queue
     * @param pose the current GUI pose matrix, used for coordinate transformation
     * @param x0 left screen coordinate in logical GUI pixels
     * @param y0 top screen coordinate in logical GUI pixels
     * @param x1 right screen coordinate in logical GUI pixels
     * @param y1 bottom screen coordinate in logical GUI pixels
     * @param alpha ARGB-packed alpha multiplier; use -1 for fully opaque white
     * @param scissor optional scissor rectangle to clip the blit
     */
    fun submitBlit(
        key: SkyHanniRoundedShapeAtlasKey,
        guiRenderState: GuiRenderState,
        pose: Matrix3x2f,
        x0: Int, y0: Int, x1: Int, y1: Int,
        alpha: Int,
        scissor: ScreenRectangle?,
    ): Boolean {
        val entry = entries[key] ?: return false
        val size = sizePixels.toFloat()
        // Strip the slot padding from the UV so the content maps 1:1 to the screen rect.
        // entry.u/v is the top-left of the slot; content starts PADDING pixels inside.
        val pad = PADDING / size
        val uContent = entry.u + pad
        val vContent = entry.v - pad
        val u1 = uContent + key.pixelWidth / size
        val v1 = vContent - key.pixelHeight / size
        guiRenderState.addBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                @Suppress("UnsafeCallOnNullableType")
                TextureSetup.singleTexture(textureView!!, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                pose,
                x0, y0, x1, y1,
                uContent, u1, vContent, v1,
                alpha,
                scissor,
            )
        )
        return true
    }

    override fun close() {
        super.close()
    }
}
