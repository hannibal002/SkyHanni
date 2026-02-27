package at.hannibal2.skyhanni.utils.renderables.animated

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable
import net.minecraft.core.Direction.Axis
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import kotlin.math.sin
import kotlin.time.Duration

/**
 * A data class that defines the bouncing behavior of an item stack.
 * The render will start in the 'middle' and will move up/down relative to that position.
 *
 * @param upwardBounce The upward bounce distance in pixels.
 * @param downwardBounce The downward bounce distance in pixels.
 * @param bounceSpeed How many pixels the item should move per second.
 */
data class ItemStackBounceDefinition(
    val upwardBounce: Int = 0,
    val downwardBounce: Int = 0,
    val bounceSpeed: Double = 0.0,
) {
    fun isEnabled() = (upwardBounce > 0 || downwardBounce > 0) && bounceSpeed > 0.0
    fun getTotalBounceHeight(): Int = upwardBounce + downwardBounce
}

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
    val axis: Axis = Axis.Y,
    val rotationSpeed: Double = 0.0,
)

/**
 * A class that defines behavior for a 'frame' of an ItemStack animation.
 *
 * A ticks parameter of 0 will make the frame last permanently.
 *
 * @param stack The ItemStack that should render during this frame.
 * @param ticks How long this frame should last, in ticks (assuming a nominal 20/s)
 */
class ItemStackAnimationFrame(
    private val stackProvider: () -> ItemStack,
    val ticks: Int = 0,
) {
    constructor(itemStack: ItemStack, ticks: Int = 0) : this({ itemStack }, ticks)
    constructor(provider: NeuItemStackProvider, ticks: Int = 0) : this(provider::stack, ticks)

    val stack: ItemStack get() = stackProvider()
}

class AnimatedItemStackRenderable private constructor(
    frames: Collection<ItemStackAnimationFrame>,
    private val rotationDefinition: ItemStackRotationDefinition = ItemStackRotationDefinition(),
    initialRotation: Vec3 = Vec3(0.0, 0.0, 0.0),
    private val bounceDefinition: ItemStackBounceDefinition = ItemStackBounceDefinition(),
    scale: Double = NeuItems.ITEM_FONT_SIZE,
    xSpacing: Int = 2,
    ySpacing: Int = 1,
    rescaleSkulls: Boolean = true,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
    private val rotationSync: ((Vec3) -> Unit)? = null,
) : ItemStackRenderable(
    {
        frames.firstOrNull()?.stack ?: ErrorManager.skyHanniError(
            "Cannot initialize AnimatedItemStackRenderable with an empty animation context.",
        )
    },
    scale,
    xSpacing,
    ySpacing,
    rescaleSkulls,
    horizontalAlign,
    verticalAlign,
),
    TimeDependentRenderable {
    private var frameIndex = 0
    private var ticksInFrame = 0.0
    private val frameDefs = frames.toList()
    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()

    private val startTime = SimpleTimeMark.now()
    private val baseItemHeight = (15.5 * scale + 0.5).toInt() + ySpacing
    private val fullBounceHeight = if (bounceDefinition.isEnabled()) bounceDefinition.getTotalBounceHeight() else 0
    private val bounceOffset = fullBounceHeight / 2.0

    override val height = baseItemHeight + fullBounceHeight
    override val stack: ItemStack get() = frameDefs[frameIndex].stack

    private var currentRotation: Vec3 = initialRotation
    private fun generateNextRotation(deltaTime: Double): Vec3 = Vec3(
        currentRotation.x + when (rotationDefinition.axis) {
            Axis.X -> rotationDefinition.rotationSpeed * deltaTime
            else -> 0.0
        },
        currentRotation.y + when (rotationDefinition.axis) {
            Axis.Y -> rotationDefinition.rotationSpeed * deltaTime
            else -> 0.0
        },
        currentRotation.z + when (rotationDefinition.axis) {
            Axis.Z -> rotationDefinition.rotationSpeed * deltaTime
            else -> 0.0
        },
    ).also { rotationSync?.invoke(currentRotation) }

    private fun ItemStackBounceDefinition.calculateBounce(): Double {
        if (!bounceDefinition.isEnabled()) return 0.0

        val t = startTime.passedSince().inPartialSeconds
        val period = fullBounceHeight * 2.0 / bounceSpeed
        val theta = (t % period) / period * (2 * Math.PI)
        val sinTheta = sin(theta)
        val pureBounce = sinTheta * (if (sinTheta >= 0) upwardBounce else downwardBounce)
        return pureBounce + bounceOffset
    }

    private fun tryMoveNextFrame(dt: Double) {
        val transitionTicks = frameDefs[frameIndex].ticks.takeIf { it > 0 } ?: return

        ticksInFrame += dt * 20.0
        if (ticksInFrame <= transitionTicks) return

        frameIndex = (frameIndex + 1) % frameDefs.size
        ticksInFrame = 0.0
    }

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        currentRotation = generateNextRotation(deltaTime.inPartialSeconds)
        val currentOffsetY = bounceDefinition.calculateBounce()
        tryMoveNextFrame(deltaTime.inPartialSeconds)

        stack.renderOnScreen(
            x = (xSpacing / 2f),
            y = currentOffsetY.toFloat(),
            scaleMultiplier = scale,
            rescaleSkulls = rescaleSkulls,
            rotationDegrees = currentRotation,
        )
    }

    @Suppress("DEPRECATION")
    @Deprecated("Use renderWithDelta instead", ReplaceWith("renderWithDelta(posX, posY, deltaTime)"))
    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) = super<TimeDependentRenderable>.render(mouseOffsetX, mouseOffsetY)

    companion object {
        fun Renderable.Companion.animatedItemStack(
            frames: Collection<ItemStackAnimationFrame>,
            rotationDefinition: ItemStackRotationDefinition = ItemStackRotationDefinition(),
            initialRotation: Vec3 = Vec3(0.0, 0.0, 0.0),
            bounceDefinition: ItemStackBounceDefinition = ItemStackBounceDefinition(),
            scale: Double = NeuItems.ITEM_FONT_SIZE,
            xSpacing: Int = 2,
            ySpacing: Int = 1,
            rescaleSkulls: Boolean = true,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
            rotationSync: ((Vec3) -> Unit)? = null,
        ) = AnimatedItemStackRenderable(
            frames, rotationDefinition, initialRotation, bounceDefinition, scale, xSpacing, ySpacing,
            rescaleSkulls, horizontalAlign, verticalAlign, rotationSync
        )
    }
}
