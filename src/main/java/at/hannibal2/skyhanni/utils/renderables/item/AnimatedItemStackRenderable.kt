package at.hannibal2.skyhanni.utils.renderables.item

import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.inPartialSeconds
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing.Axis
import net.minecraft.util.Vec3
import kotlin.math.roundToInt

/**
 * A data class that defines the bouncing behavior of an item stack.
 * The render will start in the 'middle' and will move up/down relative to that position.
 *
 * @param upwardBounce The upward bounce distance in pixels.
 * @param downwardBounce The downward bounce distance in pixels.
 * @param bounceSpeed How many pixels the item should move per second.
 */
data class ItemStackBounceDefinition(
    val upwardBounce: Int,
    val downwardBounce: Int,
    val bounceSpeed: Double = 0.5,
)

private data class LocalPosition(val x: Double, val y: Double)

private enum class BounceVelocity { UP, DOWN, NONE }

/**
 * A data class that defines the rotation behavior of an item stack.
 *
 * A positive rotation speed will rotate the item counter-clockwise,
 * a negative rotation speed will rotate it clockwise, and a
 * rotation speed of 0.0 will make the item stationary.
 *
 * @param axis The axis around which the item stack will rotate.
 * @param rotationSpeed How many degrees the item should rotate per second.
 */
data class ItemStackRotationDefinition(
    val axis: Axis,
    val rotationSpeed: Double,
)

class AnimatedItemStackRenderable(
    item: ItemStack,
    private val rotation: ItemStackRotationDefinition = ItemStackRotationDefinition(
        axis = Axis.Y,
        rotationSpeed = 0.0,
    ),
    private val bounce: ItemStackBounceDefinition = ItemStackBounceDefinition(
        upwardBounce = 0,
        downwardBounce = 0,
    ),
    scale: Double = NeuItems.ITEM_FONT_SIZE,
    xSpacing: Int = 2,
    ySpacing: Int = 1,
    rescaleSkulls: Boolean = true,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
    override val highlight: Boolean = false,
): ItemStackRenderable(
    item,
    scale,
    xSpacing,
    ySpacing,
    rescaleSkulls,
    horizontalAlign,
    verticalAlign,
    highlight,
) {
    override val height = (15.5 * scale + 0.5).toInt() + ySpacing + bounce.upwardBounce + bounce.downwardBounce

    private var lastTime: SimpleTimeMark = SimpleTimeMark.now()

    private var currentPosition: LocalPosition = LocalPosition(0.0, 0.0)
    private fun generateNextPosition(deltaTime: Double): LocalPosition = LocalPosition(
        x = currentPosition.x,
        y = currentPosition.y + when (currentBounceVelocity) {
            BounceVelocity.UP -> bounce.upwardBounce * deltaTime
            BounceVelocity.DOWN -> -bounce.downwardBounce * deltaTime
            BounceVelocity.NONE -> 0
        }.toDouble(),
    )

    private var currentRotation: Vec3 = Vec3(0.0, 0.0, 0.0)
    private fun generateNextRotation(deltaTime: Double): Vec3 = Vec3(
        currentRotation.xCoord + when (rotation.axis) {
            Axis.X -> rotation.rotationSpeed * deltaTime
            else -> 0.0
        },
        currentRotation.yCoord + when (rotation.axis) {
            Axis.Y -> rotation.rotationSpeed * deltaTime
            else -> 0.0
        },
        currentRotation.zCoord + when (rotation.axis) {
            Axis.Z -> rotation.rotationSpeed * deltaTime
            else -> 0.0
        },
    )

    private var currentBounceVelocity = when {
        bounce.upwardBounce > 0 -> BounceVelocity.UP
        bounce.downwardBounce > 0 -> BounceVelocity.DOWN
        else -> BounceVelocity.NONE
    }
    private fun generateNextBounceVelocity() = when {
        currentPosition.y >= bounce.upwardBounce -> BounceVelocity.DOWN
        currentPosition.y <= bounce.downwardBounce -> BounceVelocity.UP
        else -> currentBounceVelocity
    }

    override fun render(posX: Int, posY: Int) {
        val now = SimpleTimeMark.now()
        val deltaTime = now - lastTime
        lastTime = now

        currentRotation = generateNextRotation(deltaTime.inPartialSeconds)
        currentBounceVelocity = generateNextBounceVelocity()
        currentPosition = generateNextPosition(deltaTime.inPartialSeconds)

        val (x, y) = currentPosition.x.roundToInt() to currentPosition.y.roundToInt()
        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate(x.toFloat(), y.toFloat(), 0f)
        stack.renderOnScreen(
            (xSpacing / 2.0f) + x,
            y.toFloat(),
            scaleMultiplier = scale,
            rescaleSkulls,
            rotationDegrees = currentRotation,
        )
        DrawContextUtils.popMatrix()
    }
}
