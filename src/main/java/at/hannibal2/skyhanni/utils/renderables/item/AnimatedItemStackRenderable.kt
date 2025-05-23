package at.hannibal2.skyhanni.utils.renderables.item

import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing.Axis
import net.minecraft.util.Vec3
import kotlin.math.sin

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
) : ItemStackRenderable(
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
    private var lastTime = SimpleTimeMark.now()
    private val startTime = SimpleTimeMark.now()

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

    override fun render(posX: Int, posY: Int) {
        val t = (SimpleTimeMark.now() - startTime).inPartialSeconds
        // time to go up _and_ down once (seconds):
        val period = (bounce.upwardBounce + bounce.downwardBounce) * 2 / bounce.bounceSpeed
        // angle from 0 → 2π over one full cycle
        val theta = (t % period) / period * (2 * Math.PI)
        // build an asymmetric sine:
        //   maps -1..1  →  -downwardBounce...upwardBounce
        val amplitude = bounce.upwardBounce + bounce.downwardBounce
        val offset = sin(theta) * (amplitude / 2)
        val rest = (bounce.upwardBounce - bounce.downwardBounce) / 2
        val currentOffsetY = offset + rest

        val dt = (SimpleTimeMark.now() - lastTime).inPartialSeconds
        lastTime = SimpleTimeMark.now()
        currentRotation = generateNextRotation(dt)

        stack.renderOnScreen(
            x = (posX + (xSpacing / 2f)),
            y = (posY + currentOffsetY).toFloat(),
            scaleMultiplier = scale,
            rescaleSkulls = rescaleSkulls,
            rotationDegrees = currentRotation
        )
    }
}
