package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledCircle
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import java.awt.Color

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
    private val radius: Int,
    filledPercentage: Double = 100.0,
    unfilledColor: Color = Color.LIGHT_GRAY,
    /**
     * Indicates if the renderable should be centered in the circle, or use the padding property.
     * If set to true, containerPadding is unused.
     */
    private val centered: Boolean = false,
    /**
     * How much vertical and horizontal padding should there be from the top left before the renderable renders.
     * Only applies when centered is false.
     */
    private val containerPadding: Float = 2.0f,
) : CircularRenderable(backgroundColor, radius, filledPercentage, unfilledColor) {
    override fun render(posX: Int, posY: Int) {
        drawCircle(posX, posY)
        val translationX = if (centered) (radius - renderable.width / 2f) else containerPadding
        val translationY = if (centered) (radius - renderable.height / 2f) else containerPadding
        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate(posX + translationX, posY + translationY, 0f)
        renderable.render(0, 0)
        DrawContextUtils.popMatrix()
    }
}
