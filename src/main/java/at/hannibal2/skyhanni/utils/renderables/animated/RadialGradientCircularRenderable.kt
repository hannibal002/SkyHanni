package at.hannibal2.skyhanni.utils.renderables.animated

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.render.ShaderRenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.CircularRenderable
import io.github.notenoughupdates.moulconfig.ChromaColour
import kotlin.time.Duration

class RadialGradientCircularRenderable private constructor(
    radius: Int,
    private val startColor: ChromaColour,
    private val endColor: ChromaColour,
    private val smoothness: Float = 1f,
    private val gradientAngle: Float = 180f,
    private val gradientSpeed: Float = 0.5f,
    private val reverse: Boolean = false,
    phaseOffSet: Float = 0f,
    gradientProgress: Float = 0f,
    horizontalAlignment: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.CENTER,
    verticalAlignment: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
) : CircularRenderable(
    startColor, radius, smoothness, 100.0, endColor, horizontalAlignment, verticalAlignment
),
    TimeDependentRenderable {
    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()
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

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        val dt = deltaTime.inPartialSeconds
        gradientProgress = generateNextGradientProgress(dt)
        phaseOffset = generateNextPhaseOffset(dt)

        ShaderRenderUtils.drawRadialGradientFilledCircle(
            x = mouseOffsetX,
            y = mouseOffsetY,
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

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) = super<TimeDependentRenderable>.render(mouseOffsetX, mouseOffsetY)

    companion object {
        fun Renderable.Companion.radialGradientCircular(
            radius: Int,
            startColor: ChromaColour,
            endColor: ChromaColour,
            smoothness: Float = 1f,
            gradientAngle: Float = 180f,
            gradientSpeed: Float = 0.5f,
            reverse: Boolean = false,
            phaseOffSet: Float = 0f,
            gradientProgress: Float = 0f,
            horizontalAlignment: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlignment: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
        ) = RadialGradientCircularRenderable(
            radius,
            startColor,
            endColor,
            smoothness,
            gradientAngle,
            gradientSpeed,
            reverse,
            phaseOffSet,
            gradientProgress,
            horizontalAlignment,
            verticalAlignment
        )
    }
}
