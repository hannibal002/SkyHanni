package at.hannibal2.skyhanni.utils.renderables.animated.framed

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedItemRenderableConfig
import at.hannibal2.skyhanni.utils.renderables.animated.TimeDependentRenderable
import at.hannibal2.skyhanni.utils.renderables.decorators.RenderableDecorator

internal interface FramedBehavior<T : AnimatedFrame> : AnimatedFrameStorage<T> {
    val config: AnimatedItemRenderableConfig<T>
    val frameStorage: AnimatedFrameStorage<T> get() = config.frameStorage
    override val frames: List<T> get() = frameStorage.frames
    override val tickRateProvider: FrameTickRateProvider get() = frameStorage.tickRateProvider
    override var currentFrameIndex: Int
        get() = frameStorage.currentFrameIndex
        set(value) {
            frameStorage.currentFrameIndex = value
        }

    val currentFrame get() = frames[currentFrameIndex]

    var stableRenderId: Int?
    var ticksInFrame: Double
    fun tryMoveNextFrame(dt: Double) {
        val transitionTicks = this.tickRateProvider.getTransitionTicks(currentFrame).takeIf { it > 0 } ?: return

        ticksInFrame += dt * 20.0
        if (ticksInFrame <= transitionTicks) return

        currentFrameIndex = (currentFrameIndex + 1) % frames.size
        ticksInFrame = 0.0
    }
}

class FramedRenderable<T : AnimatedFrame> private constructor(
    override val root: Renderable,
    override val config: AnimatedItemRenderableConfig<T>,
) : RenderableDecorator, TimeDependentRenderable, FramedBehavior<T> {
    override val height: Int get() = root.height
    override val width: Int get() = root.width
    override val horizontalAlign get() = root.horizontalAlign
    override val verticalAlign get() = root.verticalAlign

    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()
    override var ticksInFrame: Double = 0.0
    override var stableRenderId: Int? = null

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: kotlin.time.Duration) {
        tryMoveNextFrame(deltaTime.inPartialSeconds)
    }

    companion object {
        fun <T : AnimatedFrame> Renderable.Companion.framed(
            root: Renderable,
            config: AnimatedItemRenderableConfig<T>.() -> Unit = { }
        ) = FramedRenderable(root, AnimatedItemRenderableConfig<T>().apply(config))
    }
}
