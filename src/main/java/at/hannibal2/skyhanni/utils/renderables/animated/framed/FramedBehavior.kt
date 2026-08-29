package at.hannibal2.skyhanni.utils.renderables.animated.framed

import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedItemRenderableConfig

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
