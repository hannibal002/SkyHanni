package at.hannibal2.skyhanni.utils.renderables.primitives

import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.render.ShaderRenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.notenoughupdates.moulconfig.ChromaColour
import java.awt.Color

open class CircularRenderable protected constructor(
    private val backgroundColor: ChromaColour,
    val radius: Int,
    private val smoothness: Float = 1f,
    private val filledPercentage: Double = 100.0,
    private val unfilledColor: ChromaColour = Color.LIGHT_GRAY.toChromaColor(255),
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
) : Renderable {
    override val width: Int = radius * 2
    override val height: Int = radius * 2
    override val horizontalAlign = horizontalAlignment
    override val verticalAlign = verticalAlignment

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) = when {
        filledPercentage < 100.0 -> {
            val baseAngle = Math.PI.toFloat() * 3f / 2f
            val endAngle = (baseAngle + ((100.0 - filledPercentage) / 50.0 * Math.PI).toFloat()).mod(2f * Math.PI.toFloat())
            ShaderRenderUtils.drawFilledCircle(0, 0, backgroundColor.toColor(), radius, smoothness, baseAngle, endAngle)
            ShaderRenderUtils.drawFilledCircle(0, 0, unfilledColor.toColor(), radius, smoothness, endAngle, baseAngle)
        }

        else -> ShaderRenderUtils.drawFilledCircle(0, 0, backgroundColor.toColor(), radius, smoothness = smoothness)
    }

    companion object {
        fun Renderable.Companion.circular(
            backgroundColor: ChromaColour,
            radius: Int,
            smoothness: Float = 1f,
            filledPercentage: Double = 100.0,
            unfilledColor: ChromaColour = Color.LIGHT_GRAY.toChromaColor(255),
            horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
            verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
        ) = CircularRenderable(backgroundColor, radius, smoothness, filledPercentage, unfilledColor, horizontalAlignment, verticalAlignment)
    }
}

