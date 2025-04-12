package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.inPartialSeconds
import net.minecraft.client.renderer.GlStateManager
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * This is a wrapper for a renderable that moves across the entire display and hits the edges and senses when the edge is hit.
 * Just like the old DVD logos on old monitors back in the old days. old.
 */

class DVDLogoRenderable(
    private val renderable: Renderable,
    private var velocity: LogoVelocity = LogoVelocity.entries.random(),
    override val horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.CENTER,
    override val verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
    private val onBounce: (Renderable) -> Unit = {},
    private val onCornerHit: (Renderable) -> Unit = {},
) : Renderable {
    override val width: Int = renderable.width
    override val height: Int = renderable.height

    private var lastTime: SimpleTimeMark = SimpleTimeMark.now()

    private var position: Position = renderable.generateRandomStartingPosition()

    private fun generateNextVelocity(
        posXAtEdge: Boolean,
        posXAtLeftEdge: Boolean,
        posYAtEdge: Boolean,
        posYAtTopEdge: Boolean,
    ): LogoVelocity = when {
        posXAtEdge && posYAtEdge -> {
            onCornerHit.invoke(this.renderable)
            velocity.invert()
        }

        posXAtEdge -> {
            onBounce.invoke(this.renderable)
            velocity.applyApplicator(
                if (posXAtLeftEdge) LogoVelocity.ApplicatorDirection.RIGHT
                else LogoVelocity.ApplicatorDirection.LEFT,
            )
        }

        posYAtEdge -> {
            onBounce.invoke(this.renderable)
            velocity.applyApplicator(
                if (posYAtTopEdge) LogoVelocity.ApplicatorDirection.DOWN
                else LogoVelocity.ApplicatorDirection.UP,
            )
        }

        else -> velocity
    }

    private fun generateNextPosition(deltaTime: Double): Position = Position(
        x = position.x + velocity.x * deltaTime,
        y = position.y + velocity.y * deltaTime,
    )

    override fun render(posX: Int, posY: Int) {
        val now = SimpleTimeMark.now()
        val deltaTime = now - lastTime
        lastTime = now

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

        velocity = generateNextVelocity(posXAtEdge, posXAtLeftEdge, posYAtEdge, posYAtTopEdge)
        position = generateNextPosition(deltaTime.inPartialSeconds)

        val (x, y) = position.x.roundToInt() to position.y.roundToInt()
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        renderable.render(posX + x, posY + y)
        GlStateManager.popMatrix()
    }
}

enum class LogoVelocity(var x: Int, val y: Int) {
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

    fun invert(): LogoVelocity = of(-x, -y)

    fun applyApplicator(direction: ApplicatorDirection): LogoVelocity = when (direction) {
        ApplicatorDirection.LEFT -> of(-1 * abs(x), y)
        ApplicatorDirection.RIGHT -> of(abs(x), y)
        ApplicatorDirection.UP -> of(x, -1 * abs(y))
        ApplicatorDirection.DOWN -> of(x, abs(y))
    }

    companion object {
        private fun of(x: Int, y: Int): LogoVelocity = when {
            x > 0 && y < 0 -> UP_RIGHT
            x < 0 && y < 0 -> UP_LEFT
            x > 0 && y > 0 -> DOWN_RIGHT
            x < 0 && y > 0 -> DOWN_LEFT
            else -> throw IllegalArgumentException("Invalid velocity: ($x, $y)")
        }
    }
}

private fun Renderable.generateRandomStartingPosition() = Position(
    x = (0..(GuiScreenUtils.scaledWindowWidth - (width * 2))).random().toDouble(),
    y = (0..(GuiScreenUtils.scaledWindowHeight - (height * 2))).random().toDouble(),
)

private class Position(val x: Double, val y: Double)
