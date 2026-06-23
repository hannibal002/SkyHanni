package at.hannibal2.skyhanni.utils.render.states

import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats.writeParams
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.resources.Identifier

class SkyHanniRoundedTexturedRectRenderState(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    params: RoundedRenderStateParams,
    private val smoothness: Float,
    private val texture: Identifier,
    private val alpha: Float,
    scissor: ScreenRectangle?,
) : AbstractSkyHanniRoundedShapeRenderState(x, y, width, height, params, scissor) {

    override val padding = 5
    override fun pipeline() = SkyHanniRenderPipeline.ROUNDED_TEXTURED_RECT_DEFERRED()

    override fun textureSetup(): TextureSetup {
        val fetchTexture = Minecraft.getInstance().textureManager.getTexture(texture)
        val view = fetchTexture.textureView
        return TextureSetup.singleTexture(view, fetchTexture.sampler)
    }

    override fun buildVertices(consumer: VertexConsumer) {
        val p = padding.toFloat()
        writeTexturedVertex(consumer, (x - p), (y - p), 0f, 0f)
        writeTexturedVertex(consumer, (x - p), (y + height + p), 0f, 1f)
        writeTexturedVertex(consumer, (x + width + p), (y + height + p), 1f, 1f)
        writeTexturedVertex(consumer, (x + width + p), (y - p), 1f, 0f)
    }

    private fun writeTexturedVertex(consumer: VertexConsumer, vx: Float, vy: Float, u: Float, v: Float) = with(params) {
        val buf = consumer as BufferBuilder
        buf.addVertex(matXScale * vx + matXTranslation, matYScale * vy + matYTranslation, 0f)
        buf.setUv(u, v)
        buf.writeParams(radius, smoothness, adjustedHalfSizeX, adjustedHalfSizeY, SkyHanniVertexFormats.VertexElement.ROUNDED_PARAMS_0)
        buf.writeParams(adjustedCenterPosX, adjustedCenterPosY, alpha, 0f, SkyHanniVertexFormats.VertexElement.ROUNDED_PARAMS_1)
    }
}
