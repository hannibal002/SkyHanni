package at.hannibal2.skyhanni.utils.render.states

import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats.writeParams
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle

class SkyHanniRadialGradientCircleRenderState(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    params: RoundedRenderStateParams,
    private val smoothness: Float,
    private val angle: Float,
    private val progress: Float,
    private val phaseOffset: Float,
    private val reverse: Boolean,
    private val startColor: FloatArray,
    private val endColor: FloatArray,
    scissor: ScreenRectangle?,
) : AbstractSkyHanniRoundedShapeRenderState(x, y, width, height, params, scissor) {

    override val padding = 5
    override fun pipeline() = SkyHanniRenderPipeline.RADIAL_GRADIENT_CIRCLE_DEFERRED()

    override fun buildVertices(consumer: VertexConsumer) {
        val p = padding.toFloat()
        writeGradientVertex(consumer, (x - p), (y - p))
        writeGradientVertex(consumer, (x - p), (y + height + p))
        writeGradientVertex(consumer, (x + width + p), (y + height + p))
        writeGradientVertex(consumer, (x + width + p), (y - p))
    }

    private fun writeGradientVertex(consumer: VertexConsumer, vx: Float, vy: Float) = with(params) {
        val buf = consumer as BufferBuilder
        buf.addVertex(matXScale * vx + matXTranslation, matYScale * vy + matYTranslation, 0f)
        buf.writeParams(radius, smoothness, adjustedHalfSizeX, adjustedHalfSizeY, SkyHanniVertexFormats.VertexElement.ROUNDED_PARAMS_0)
        buf.writeParams(adjustedCenterPosX, adjustedCenterPosY, 0f, 0f, SkyHanniVertexFormats.VertexElement.ROUNDED_PARAMS_1)
        buf.writeParams(angle, progress, phaseOffset, if (reverse) 1f else 0f, SkyHanniVertexFormats.VertexElement.GRADIENT_PARAMS_0)
        buf.writeParams(startColor[0], startColor[1], startColor[2], startColor[3], SkyHanniVertexFormats.VertexElement.GRADIENT_PARAMS_1)
        buf.writeParams(endColor[0], endColor[1], endColor[2], endColor[3], SkyHanniVertexFormats.VertexElement.GRADIENT_PARAMS_2)
    }
}
