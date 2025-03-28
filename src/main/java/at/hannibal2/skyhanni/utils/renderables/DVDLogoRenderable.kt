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

    enum class ApplicatorDirection { LEFT, RIGHT, UP, DOWN }

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

    private fun generateNextVelocity(
        posXAtEdge: Boolean,
        posXAtLeftEdge: Boolean,
        posYAtEdge: Boolean,
        posYAtTopEdge: Boolean
    ): LogoVelocity = when {
        posXAtEdge && posYAtEdge -> {
            onCornerHit.invoke(this.renderable)
            velocity.invert()
        }
        posXAtEdge -> {
            onBounce.invoke(this.renderable)
            velocity.applyApplicator(
                if (posXAtLeftEdge) LogoVelocity.ApplicatorDirection.RIGHT
                else LogoVelocity.ApplicatorDirection.LEFT
            )
        }
        posYAtEdge -> {
            onBounce.invoke(this.renderable)
            velocity.applyApplicator(
                if (posYAtTopEdge) LogoVelocity.ApplicatorDirection.DOWN
                else LogoVelocity.ApplicatorDirection.UP
            )
        }
        else -> velocity
    }

    private fun generateNextPosition(): Position = Position(
        x = position.x + velocity.x,
        y = position.y + velocity.y
    )

    override fun render(posX: Int, posY: Int) {
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
        position = generateNextPosition()

        GlStateManager.pushMatrix()
        GlStateManager.translate(position.x.toFloat(), position.y.toFloat(), 0f)
        renderable.render(posX + position.x, posY + position.y)
        GlStateManager.popMatrix()
    }
}
