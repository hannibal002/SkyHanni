package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledCircle
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import java.awt.Color
import kotlin.math.max

open class CircularRenderable(
    private val backgroundColor: Color,
    val radius: Int,
    private val filledPercentage: Double = 100.0,
    private val unfilledColor: Color = Color.LIGHT_GRAY,
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
) : Renderable {
    override val width: Int = radius * 2
    override val height: Int = radius * 2
    override val horizontalAlign = horizontalAlignment
    override val verticalAlign = verticalAlignment

    override fun render(posX: Int, posY: Int) {
        renderCircle(posX, posY)
    }

    private fun renderCircle(posX: Int, posY: Int) = when {
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
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
    private val padding: Int = 2,
) : CircularRenderable(
    backgroundColor,
    radius = (max(renderable.width, renderable.height) / 2) + padding,
    filledPercentage,
    unfilledColor,
    horizontalAlignment,
    verticalAlignment,
) {
    private val takenSpace = 2 * (radius - padding)
    override fun render(posX: Int, posY: Int) {
        super.render(posX, posY)
        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate(padding.toFloat(), padding.toFloat(), 0f)
        renderable.renderXYAligned(posX + padding, posY + padding, takenSpace, takenSpace)
        DrawContextUtils.popMatrix()
    }
}
