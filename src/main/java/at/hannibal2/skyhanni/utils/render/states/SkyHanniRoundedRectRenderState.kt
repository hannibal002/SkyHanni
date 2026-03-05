package at.hannibal2.skyhanni.utils.render.states

import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.render.SHVFE
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats.writeParams
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle

class SkyHanniRoundedRectRenderState(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val color: Int,
    private val smoothness: Float,
    params: RoundedRenderStateParams,
    scissor: ScreenRectangle?,
) : AbstractSkyHanniRoundedRectRenderState(x, y, width, height, params, scissor) {

    override val padding = 5
    override fun pipeline() = SkyHanniRenderPipeline.ROUNDED_RECT_DEFERRED()

    override fun writeVertex(consumer: VertexConsumer, vx: Float, vy: Float, isTop: Boolean) = with(params) {
        val buf = consumer as BufferBuilder
        buf.addVertex(matXScale * vx + matXTranslation, matYScale * vy + matYTranslation, 0f)
        buf.setColor(color)
        buf.writeParams(radius, smoothness, adjustedHalfSizeX, adjustedHalfSizeY, SHVFE.ROUNDED_PARAMS_0)
        buf.writeParams(adjustedCenterPosX, adjustedCenterPosY, 0f, 0f, SHVFE.ROUNDED_PARAMS_1)
    }
}
