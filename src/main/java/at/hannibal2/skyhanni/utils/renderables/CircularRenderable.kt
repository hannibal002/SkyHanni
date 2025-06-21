package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.render.ShaderRenderUtils
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import io.github.notenoughupdates.moulconfig.ChromaColour
import java.awt.Color
import kotlin.math.max

open class CircularRenderable(
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

    override fun render(posX: Int, posY: Int) = when {
        filledPercentage < 100.0 -> {
            val baseAngle = Math.PI.toFloat() * 3f / 2f
            val endAngle = (baseAngle + ((100.0 - filledPercentage) / 50.0 * Math.PI).toFloat()).mod(2f * Math.PI.toFloat())
            ShaderRenderUtils.drawFilledCircle(0, 0, backgroundColor.toColor(), radius, smoothness, baseAngle, endAngle)
            ShaderRenderUtils.drawFilledCircle(0, 0, unfilledColor.toColor(), radius, smoothness, endAngle, baseAngle)
        }
        else -> ShaderRenderUtils.drawFilledCircle(0, 0, backgroundColor.toColor(), radius, smoothness = smoothness)
    }
}

class RadialGradientCircularRenderable(
    radius: Int,
    private val startColor: ChromaColour,
    private val endColor: ChromaColour,
    private val smoothness: Float = 1f,
    private val gradientAngle: Float = 180f,
    private val gradientSpeed: Float = 0.5f,
    private val reverse: Boolean = false,
    phaseOffSet: Float = 0f,
    gradientProgress: Float = 0f,
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
) : CircularRenderable(startColor, radius, smoothness, 100.0, endColor, horizontalAlignment, verticalAlignment) {
    private var lastTime = SimpleTimeMark.now()
    private var gradientProgress: Float = gradientProgress % 360f // Bounded to 0 -> 360
    private var phaseOffset: Float = phaseOffSet % 1f // Bounded to 0 -> 1

    private fun generateNextGradientProgress(deltaTime: Double): Float {
        gradientProgress += (gradientSpeed * deltaTime).toFloat()
        gradientProgress %= 360f
        return gradientProgress
    }

    private fun generateNextPhaseOffset(deltaTime: Double): Float {
        phaseOffset += (gradientSpeed * deltaTime).toFloat()
        phaseOffset %= 1f
        return phaseOffset
    }

    override fun render(posX: Int, posY: Int) {
        val dt = (SimpleTimeMark.now() - lastTime).inPartialSeconds
        lastTime = SimpleTimeMark.now()
        gradientProgress = generateNextGradientProgress(dt)
        phaseOffset = generateNextPhaseOffset(dt)

        ShaderRenderUtils.drawRadialGradientFilledCircle(
            x = posX,
            y = posY,
            radius = radius,
            startColor = startColor,
            endColor = endColor,
            progress = gradientProgress,
            phaseOffset = phaseOffset,
            smoothness = smoothness,
            angle = gradientAngle,
            reverse = reverse,
        )
    }
}

class CircularContainerRenderable(
    private val renderable: Renderable,
    backgroundColor: ChromaColour,
    smoothness: Float = 1f,
    filledPercentage: Double = 100.0,
    unfilledColor: ChromaColour = Color.LIGHT_GRAY.toChromaColor(255),
    horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
    verticalAlignment: VerticalAlignment = VerticalAlignment.CENTER,
    private val padding: Int = 2,
) : CircularRenderable(
    backgroundColor,
    radius = (max(renderable.width, renderable.height) / 2) + padding,
    smoothness,
    filledPercentage,
    unfilledColor,
    horizontalAlignment,
    verticalAlignment,
) {
    private val takenSpace = 2 * (radius - padding)
    override fun render(posX: Int, posY: Int) {
        super.render(posX, posY)
        DrawContextUtils.translated(padding.toFloat(), padding.toFloat(), 0f) {
            renderable.renderXYAligned(posX + padding, posY + padding, takenSpace, takenSpace)
        }
    }
}
