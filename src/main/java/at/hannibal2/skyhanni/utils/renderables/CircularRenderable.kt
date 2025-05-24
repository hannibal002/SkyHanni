package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledCircle
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import java.awt.Color
import kotlin.math.max

open class CircularRenderable(
    private val backgroundColor: Color,
    private val radius: Int,
    private val filledPercentage: Double = 100.0,
    private val unfilledColor: Color = Color.LIGHT_GRAY,
) : Renderable {
    override val width: Int = radius * 2
    override val height: Int = radius * 2
    override val horizontalAlign = HorizontalAlignment.LEFT
    override val verticalAlign = VerticalAlignment.TOP

    override fun render(posX: Int, posY: Int) {
        drawCircle(posX, posY)
    }

    protected fun drawCircle(posX: Int, posY: Int) = when {
        filledPercentage < 100.0 -> {
            val baseAngle = Math.PI.toFloat() * 3f / 2f
            val endAngle = (baseAngle + ((100.0 - filledPercentage) / 50.0 * Math.PI).toFloat()).mod(2f * Math.PI.toFloat())
            drawFilledCircle(posX, posY, radius, backgroundColor, angle1 = baseAngle, angle2 = endAngle)
            drawFilledCircle(posX, posY, radius, unfilledColor, angle1 = endAngle, angle2 = baseAngle)
        }
        else -> drawFilledCircle(posX, posY, radius, backgroundColor)
    }

}

class CircularContainerRenderable(
    private val renderable: Renderable,
    backgroundColor: Color,
    filledPercentage: Double = 100.0,
    unfilledColor: Color = Color.LIGHT_GRAY,
    edgePadding: Int = 2,
) : CircularRenderable(
    backgroundColor,
    max(renderable.width, renderable.height) + edgePadding,
    filledPercentage,
    unfilledColor
) {
    private val radius = max(renderable.width, renderable.height) + edgePadding
    override fun render(posX: Int, posY: Int) {
        drawCircle(posX, posY)
        renderable.renderXYAligned(0, 0, radius, radius)
    }
}
