package at.hannibal2.skyhanni.utils.renderables.animated

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.AnimatedBounceLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.AnimatedBounceStorage
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.BouncingBehavior
import at.hannibal2.skyhanni.utils.renderables.animated.framed.AnimatedFrame
import at.hannibal2.skyhanni.utils.renderables.animated.framed.AnimatedFrameLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.framed.AnimatedFrameStorage
import at.hannibal2.skyhanni.utils.renderables.animated.framed.FramedBehavior
import at.hannibal2.skyhanni.utils.renderables.animated.framed.ItemStackAnimatedFrame
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationStorage
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.RotatingBehavior
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemRenderableConfig
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable
import at.hannibal2.skyhanni.utils.system.PropertyVar
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration

class AnimatedItemStackRenderable internal constructor(
    override val config: AnimatedItemRenderableConfig<ItemStackAnimatedFrame>,
) : ItemStackRenderable(config),
    TimeDependentRenderable,
    BouncingBehavior,
    RotatingBehavior,
    FramedBehavior<ItemStackAnimatedFrame> {

    override val stack: ItemStack get() = currentFrame.stack
    override val bounceStartTime: SimpleTimeMark = SimpleTimeMark.now()
    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()
    override var ticksInFrame: Double = 0.0

    override val height: Int get() = super.height + bounceDefinition.getTotalBounceOffset(Direction.Axis.Y)
    override val width: Int get() = super.width + bounceDefinition.getTotalBounceOffset(Direction.Axis.X)

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        applyRotation(deltaTime)
        applyBounce()
        tryMoveNextFrame(deltaTime.inPartialSeconds)
        super<ItemStackRenderable>.render(mouseOffsetX, mouseOffsetY)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Use renderWithDelta instead", ReplaceWith("renderWithDelta(posX, posY, deltaTime)"))
    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) = super<TimeDependentRenderable>.render(mouseOffsetX, mouseOffsetY)

    companion object {
        fun Renderable.Companion.animatedItemStack(
            config: AnimatedItemRenderableConfig<ItemStackAnimatedFrame>.() -> Unit = { }
        ) = AnimatedItemStackRenderable(AnimatedItemRenderableConfig<ItemStackAnimatedFrame>().apply(config))
    }
}

class AnimatedItemRenderableConfig<T : AnimatedFrame> : ItemRenderableConfig() {
    var frameStorage: AnimatedFrameStorage<T> by PropertyVar(AnimatedFrameLocalStorage(emptyList()))
    var rotationStorage: AnimatedRotationStorage by PropertyVar(AnimatedRotationLocalStorage())
    var bounceStorage: AnimatedBounceStorage by PropertyVar(AnimatedBounceLocalStorage())
}
