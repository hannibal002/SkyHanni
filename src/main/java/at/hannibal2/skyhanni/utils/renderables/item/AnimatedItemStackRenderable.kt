package at.hannibal2.skyhanni.utils.renderables.item

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
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
    val upwardBounce: Int = 0,
    val downwardBounce: Int = 0,
    val bounceSpeed: Double = 0.0,
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
    val axis: Axis = Axis.Y,
    val rotationSpeed: Double = 0.0,
)

/**
 * A data class that defines behavior for a 'frame' of an ItemStack animation.
 *
 * A ticks parameter of 0 will make the frame last permanently.
 *
 * @param stack The ItemStack that should render during this frame.
 * @param ticks How long this frame should last, in ticks (assuming a nominal 20/s)
 */
data class ItemStackAnimationFrame(
    val stack: ItemStack,
    val ticks: Int = 0,
)

class AnimatedItemStackRenderable(
    frames: Collection<ItemStackAnimationFrame>,
    private val rotation: ItemStackRotationDefinition = ItemStackRotationDefinition(),
    private val bounce: ItemStackBounceDefinition = ItemStackBounceDefinition(),
    scale: Double = NeuItems.ITEM_FONT_SIZE,
    xSpacing: Int = 2,
    ySpacing: Int = 1,
    rescaleSkulls: Boolean = true,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
    override val highlight: Boolean = false,
) : ItemStackRenderable(
    frames.firstOrNull()?.stack ?: ErrorManager.skyHanniError(
        "Cannot initialize AnimatedItemStackRenderable with an empty animation context.",
    ),
    scale,
    xSpacing,
    ySpacing,
    rescaleSkulls,
    horizontalAlign,
    verticalAlign,
    highlight,
) {
    // This being unused is superfluous, basically everything besides Pet display will use this constructor
    @Suppress("unused")
    constructor(
        item: ItemStack,
        rotation: ItemStackRotationDefinition = ItemStackRotationDefinition(),
        bounce: ItemStackBounceDefinition = ItemStackBounceDefinition(),
        scale: Double = NeuItems.ITEM_FONT_SIZE,
        xSpacing: Int = 2,
        ySpacing: Int = 1,
        rescaleSkulls: Boolean = true,
        horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
        verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
        highlight: Boolean = false,
    ) : this(
        listOf(ItemStackAnimationFrame(item, 0)), rotation, bounce, scale, xSpacing,
        ySpacing, rescaleSkulls, horizontalAlign, verticalAlign, highlight,
    )

    private var frameIndex = 0
    private var ticksInFrame = 0.0
    private val frameDefs = frames.map { (itemStack, ticks) ->
        val newStack = itemStack.copy().apply {
            if (highlight) addEnchantment(EnchantmentsCompat.PROTECTION.enchantment, 1)
        }
        ItemStackAnimationFrame(newStack, ticks)
    }
    override val stack: ItemStack get() = frameDefs[frameIndex].stack

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

    private fun ItemStackBounceDefinition.calculateBounce(): Double {
        if (bounceSpeed == 0.0 || (upwardBounce == 0 && downwardBounce == 0)) return 0.0

        val t = (SimpleTimeMark.now() - startTime).inPartialSeconds
        // time to go up _and_ down once (seconds):
        val period = (upwardBounce + downwardBounce) * 2 / bounceSpeed
        // angle from 0 → 2π over one full cycle
        val theta = (t % period) / period * (2 * Math.PI)
        // build an asymmetric sine:
        //   maps -1..1  →  -downwardBounce...upwardBounce
        val amplitude = upwardBounce + downwardBounce
        val offset = sin(theta) * (amplitude / 2)
        val rest = (upwardBounce - downwardBounce) / 2
        return offset + rest
    }

    private fun tryMoveNextFrame(dt: Double) {
        val transitionTicks = frameDefs[frameIndex].ticks
        if (transitionTicks <= 0) return

        ticksInFrame += dt * 20.0
        if (ticksInFrame <= transitionTicks) return

        frameIndex = (frameIndex + 1) % frameDefs.size
        ticksInFrame = 0.0
    }

    override fun render(posX: Int, posY: Int) {
        val dt = (SimpleTimeMark.now() - lastTime).inPartialSeconds
        lastTime = SimpleTimeMark.now()
        currentRotation = generateNextRotation(dt)
        val currentOffsetY = bounce.calculateBounce()
        tryMoveNextFrame(dt)

        stack.renderOnScreen(
            x = (0 + (xSpacing / 2f)),
            y = (0 + currentOffsetY).toFloat(),
            scaleMultiplier = scale,
            rescaleSkulls = rescaleSkulls,
            rotationDegrees = currentRotation
        )
    }
}
