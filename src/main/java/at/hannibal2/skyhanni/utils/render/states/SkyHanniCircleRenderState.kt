package at.hannibal2.skyhanni.utils.render.states

import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats.writeParams
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle

class SkyHanniCircleRenderState(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val color: Int,
    private val smoothness: Float,
    private val angle1: Float,
    private val angle2: Float,
    params: RoundedRenderStateParams,
    scissor: ScreenRectangle?,
) : AbstractSkyHanniRoundedShapeRenderState(x, y, width, height, params, scissor) {

    override val padding = 5
    override fun pipeline() = SkyHanniRenderPipeline.CIRCLE_DEFERRED()

    override fun writeVertex(consumer: VertexConsumer, vx: Float, vy: Float, isTop: Boolean) = with(params) {
        val buf = consumer as BufferBuilder
        buf.addVertex(matXScale * vx + matXTranslation, matYScale * vy + matYTranslation, 0f)
        buf.setColor(color)
        buf.writeParams(radius, smoothness, adjustedHalfSizeX, adjustedHalfSizeY, SkyHanniVertexFormats.VertexElement.ROUNDED_PARAMS_0)
        buf.writeParams(adjustedCenterPosX, adjustedCenterPosY, angle1, angle2, SkyHanniVertexFormats.VertexElement.ROUNDED_PARAMS_1)
    }
}
