package at.hannibal2.skyhanni.utils.renderables.animated

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * This is a wrapper for a renderable that moves across the entire display and hits the edges and senses when the edge is hit.
 * Just like the old DVD logos on old monitors back in the old days. old.
 *
 * @param renderable The renderable to be bounced around the screen.
 * @param movementSpeed The speed in pixels per second at which the logo moves.
 * @param initialTrajectory The initial trajectory of the logo.
 * @param horizontalAlign The horizontal alignment of the logo on the screen.
 * @param verticalAlign The vertical alignment of the logo on the screen.
 * @param onBounce A callback that is invoked when the logo bounces off an edge.
 * @param onCornerHit A callback that is invoked when the logo hits a corner of the screen.
 */
class DVDLogoRenderable private constructor(
    private val renderable: Renderable,
    private val movementSpeed: Float = 4f,
    initialTrajectory: LogoTrajectory = LogoTrajectory.entries.random(),
    override val horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.CENTER,
    override val verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
    private val onBounce: (Renderable) -> Unit = {},
    private val onCornerHit: (Renderable) -> Unit = {},
) : TimeDependentRenderable {
    override val width: Int = renderable.width
    override val height: Int = renderable.height

    private var position: Position = renderable.generateRandomStartingPosition()
    private var trajectory: LogoTrajectory = initialTrajectory

    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()

    private fun generateNextTrajectory(
        posXAtEdge: Boolean,
        posXAtLeftEdge: Boolean,
        posYAtEdge: Boolean,
        posYAtTopEdge: Boolean,
    ): LogoTrajectory = when {
        posXAtEdge && posYAtEdge -> {
            onCornerHit.invoke(this.renderable)
            trajectory.invert()
        }

        posXAtEdge -> {
            onBounce.invoke(this.renderable)
            trajectory.applyApplicator(
                if (posXAtLeftEdge) LogoTrajectory.ApplicatorDirection.RIGHT
                else LogoTrajectory.ApplicatorDirection.LEFT,
            )
        }

        posYAtEdge -> {
            onBounce.invoke(this.renderable)
            trajectory.applyApplicator(
                if (posYAtTopEdge) LogoTrajectory.ApplicatorDirection.DOWN
                else LogoTrajectory.ApplicatorDirection.UP,
            )
        }

        else -> trajectory
    }

    private fun generateNextPosition(deltaTime: Double): Position = Position(
        x = position.x + (trajectory.x * movementSpeed * deltaTime),
        y = position.y + (trajectory.y * movementSpeed * deltaTime),
    )

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        val (offsetX, offsetY, _) = RenderUtils.absoluteTranslation

        val absoluteX = position.x + offsetX
        val absoluteY = position.y + offsetY

        val leftEdge = 0
        val rightEdge = GuiScreenUtils.scaledWindowWidth
        val topEdge = 0
        val bottomEdge = GuiScreenUtils.scaledWindowHeight

        val posXAtLeftEdge = absoluteX <= leftEdge
        val posXAtRightEdge = absoluteX + width >= rightEdge
        val posYAtTopEdge = absoluteY <= topEdge
        val posYAtBottomEdge = absoluteY + height >= bottomEdge

        val posXAtEdge = posXAtLeftEdge || posXAtRightEdge
        val posYAtEdge = posYAtTopEdge || posYAtBottomEdge

        trajectory = generateNextTrajectory(posXAtEdge, posXAtLeftEdge, posYAtEdge, posYAtTopEdge)
        position = generateNextPosition(deltaTime.inPartialSeconds)

        val (x, y) = position.x.roundToInt() to position.y.roundToInt()
        DrawContextUtils.pushPop {
            DrawContextUtils.translate(x.toFloat(), y.toFloat(), 0f)
            renderable.render(mouseOffsetX + x, mouseOffsetY + y)
        }
    }

    companion object {
        /**
         * Docs see: [DVDLogoRenderable]
         */
        fun Renderable.Companion.dvdLogo(
            renderable: Renderable,
            movementSpeed: Float = 4f,
            initialTrajectory: LogoTrajectory = LogoTrajectory.entries.random(),
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
            onBounce: (Renderable) -> Unit = {},
            onCornerHit: (Renderable) -> Unit = {},
        ) = DVDLogoRenderable(renderable, movementSpeed, initialTrajectory, horizontalAlign, verticalAlign, onBounce, onCornerHit)
    }
}

enum class LogoTrajectory(var x: Int, val y: Int) {
    UP_RIGHT(1, -1),
    UP_LEFT(-1, -1),
    DOWN_RIGHT(1, 1),
    DOWN_LEFT(-1, 1),
    ;

    enum class ApplicatorDirection {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    fun invert(): LogoTrajectory = of(-x, -y)

    fun applyApplicator(direction: ApplicatorDirection): LogoTrajectory = when (direction) {
        ApplicatorDirection.LEFT -> of(-1 * abs(x), y)
        ApplicatorDirection.RIGHT -> of(abs(x), y)
        ApplicatorDirection.UP -> of(x, -1 * abs(y))
        ApplicatorDirection.DOWN -> of(x, abs(y))
    }

    companion object {
        private fun of(x: Int, y: Int): LogoTrajectory = when {
            x > 0 && y < 0 -> UP_RIGHT
            x < 0 && y < 0 -> UP_LEFT
            x > 0 && y > 0 -> DOWN_RIGHT
            x < 0 && y > 0 -> DOWN_LEFT
            else -> throw IllegalArgumentException("Invalid velocity: ($x, $y)")
        }
    }
}

private fun Renderable.generateRandomStartingPosition() = Position(
    x = (0..(GuiScreenUtils.scaledWindowWidth - (width * 2)).coerceAtLeast(1)).random().toDouble(),
    y = (0..(GuiScreenUtils.scaledWindowHeight - (height * 2)).coerceAtLeast(1)).random().toDouble(),
)

private class Position(val x: Double, val y: Double)
