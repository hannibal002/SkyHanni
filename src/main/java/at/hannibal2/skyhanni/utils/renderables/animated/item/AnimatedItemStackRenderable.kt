package at.hannibal2.skyhanni.utils.renderables.animated.item

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.TimeDependentRenderable
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.AnimatedBounceStorage
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.BouncingBehavior
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationStorage
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.RotatingBehavior
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration

class AnimatedItemStackRenderable private constructor(
    override val config: AnimatedItemRenderableConfig,
    frames: Collection<ItemStackAnimationFrame>,
) : ItemStackRenderable(config, {
    frames.firstOrNull()?.stack ?: ErrorManager.skyHanniError(
        "Cannot initialize AnimatedItemStackRenderable with an empty animation context."
    )
}), TimeDependentRenderable, BouncingBehavior, RotatingBehavior {
    override val stack: ItemStack get() = frameDefs[frameIndex].stack
    override val bounceStorage: AnimatedBounceStorage get() = config.bounceStorage
    override val rotationStorage: AnimatedRotationStorage get() = config.rotationStorage
    override val bounceStartTime: SimpleTimeMark = SimpleTimeMark.now()
    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()

    private val frameDefs = frames.toList()
    private var frameIndex = 0
    private var ticksInFrame = 0.0

    private fun tryMoveNextFrame(dt: Double) {
        val transitionTicks = frameDefs[frameIndex].ticks.takeIf { it > 0 } ?: return

        ticksInFrame += dt * 20.0
        if (ticksInFrame <= transitionTicks) return

        frameIndex = (frameIndex + 1) % frameDefs.size
        ticksInFrame = 0.0
    }

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        applyRotation(deltaTime)
        applyBounce()
        tryMoveNextFrame(deltaTime.inPartialSeconds)

        stack.renderOnScreen(
            x = (xSpacing / 2f),
            y = 0f,
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
            config: AnimatedItemRenderableConfig.() -> Unit = {},
        ) = AnimatedItemStackRenderable(AnimatedItemRenderableConfig().apply(config), frames)
    }
}
