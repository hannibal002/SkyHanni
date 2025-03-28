package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import net.minecraft.client.renderer.GlStateManager
import kotlin.math.abs

enum class LogoVelocity(var x: Int, val y: Int) {
    UP_RIGHT(1, -1),
    UP_LEFT(-1, -1),
    DOWN_RIGHT(1, 1),
    DOWN_LEFT(-1, 1),
    ;

    companion object {
        fun of(x: Int, y: Int): LogoVelocity = when {
            x > 0 && y < 0 -> UP_RIGHT
            x < 0 && y < 0 -> UP_LEFT
            x > 0 && y > 0 -> DOWN_RIGHT
            x < 0 && y > 0 -> DOWN_LEFT
            else -> throw IllegalArgumentException("Invalid velocity: ($x, $y)")
        }
    }

    enum class ApplicatorDirection {
        LEFT, RIGHT, UP, DOWN
    }

    fun applyApplicator(direction: ApplicatorDirection): LogoVelocity = when (direction) {
        ApplicatorDirection.LEFT -> of(-1 * abs(x), y)
        ApplicatorDirection.RIGHT -> of(abs(x), y)
        ApplicatorDirection.UP -> of(x, -1 * abs(y))
        ApplicatorDirection.DOWN -> of(x, abs(y))
    }

    fun invert(): LogoVelocity = when (this) {
        UP_RIGHT -> DOWN_LEFT
        UP_LEFT -> DOWN_RIGHT
        DOWN_RIGHT -> UP_LEFT
        DOWN_LEFT -> UP_RIGHT
    }
}

private fun Renderable.generateRandomStartingPosition() = Position(
    x = (0..(GuiScreenUtils.scaledWindowWidth - (width * 2))).random(),
    y = (0..(GuiScreenUtils.scaledWindowHeight - (height * 2))).random()
)

class DVDLogoRenderable(
    private val renderable: Renderable,
    private var position: Position = renderable.generateRandomStartingPosition(),
    private var velocity: LogoVelocity = LogoVelocity.entries.random(),
    override val horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.CENTER,
    override val verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
    private val onBounce: (Renderable) -> Unit = {},
    private val onCornerHit: (Renderable) -> Unit = {},
) : Renderable {
    override val width: Int = renderable.width
    override val height: Int = renderable.height

    private val leftLimit = 0 + width
    private val rightLimit = GuiScreenUtils.scaledWindowWidth - width
    private val topLimit = 0 + height
    private val bottomLimit = GuiScreenUtils.scaledWindowHeight - height

    private fun generateNextVelocity(): LogoVelocity {
        val posXAtLeftEdge = (position.x <= leftLimit)
        val posXAtRightEdge = (position.x >= rightLimit)
        val posYAtTopEdge = (position.y <= topLimit)
        val posYAtBottomEdge = (position.y >= bottomLimit)
        val posXAtEdge = posXAtLeftEdge || posXAtRightEdge
        val posYAtEdge = posYAtTopEdge || posYAtBottomEdge

        return if (posXAtEdge && posYAtEdge) {
            onCornerHit.invoke(this.renderable)
            velocity.invert()
        } else if (posXAtEdge) {
            onBounce.invoke(this.renderable)
            velocity.applyApplicator(
                if (posXAtLeftEdge) LogoVelocity.ApplicatorDirection.RIGHT
                else LogoVelocity.ApplicatorDirection.LEFT
            )
        } else if (posYAtEdge) {
            onBounce.invoke(this.renderable)
            velocity.applyApplicator(
                if (posYAtTopEdge) LogoVelocity.ApplicatorDirection.DOWN
                else LogoVelocity.ApplicatorDirection.UP
            )
        } else velocity
    }

    private fun generateNextPosition(nextVelocity: LogoVelocity): Position = Position(
        x = (position.x + nextVelocity.x).coerceIn(leftLimit, rightLimit),
        y = (position.y + nextVelocity.y).coerceIn(topLimit, bottomLimit)
    )

    override fun render(posX: Int, posY: Int) {
        velocity = generateNextVelocity()
        position = generateNextPosition(velocity)

        GlStateManager.pushMatrix()
        GlStateManager.translate(position.x.toFloat(), position.y.toFloat(), 0f)

        renderable.render(posX, posY)

        GlStateManager.popMatrix()
    }
}
