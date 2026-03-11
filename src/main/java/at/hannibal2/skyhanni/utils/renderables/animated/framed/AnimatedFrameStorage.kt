package at.hannibal2.skyhanni.utils.renderables.animated.framed

import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.world.item.ItemStack

sealed interface AnimatedFrameStorage<T : AnimatedFrame> {
    val frames: List<T>
    val tickRateProvider: FrameTickRateProvider
    var currentFrameIndex: Int
}

open class AnimatedFrameLocalStorage<T : AnimatedFrame>(
    override val frames: List<T>,
    override val tickRateProvider: FrameTickRateProvider = FrameTickRateProvider.of(1.0),
) : AnimatedFrameStorage<T> {
    override var currentFrameIndex: Int = 0
}

open class AnimatedFramePropertyStorage<T : AnimatedFrame>(
    override val frames: List<T>,
    override val tickRateProvider: FrameTickRateProvider = FrameTickRateProvider.of(1.0),
    private val currentFrameIndexProvider: () -> Property<Int>,
) : AnimatedFrameStorage<T> {
    override var currentFrameIndex: Int
        get() = currentFrameIndexProvider().get()
        set(value) = currentFrameIndexProvider().set(value)
}

sealed interface AnimatedFrame {
    val transitionTicks: Int
}

/**
 * A class that defines behavior for a 'frame' of an ItemStack animation.
 *
 * A ticks parameter of 0 will make the frame last permanently.
 *
 * @param stack The ItemStack that should render during this frame.
 * @param transitionTicks How long this frame should last, in ticks (assuming a nominal 20/s)
 */
class ItemStackAnimatedFrame(
    private val stackProvider: () -> ItemStack,
    override val transitionTicks: Int = 0,
) : AnimatedFrame {
    constructor(itemStack: ItemStack, ticks: Int = 0) : this({ itemStack }, ticks)
    constructor(provider: NeuItemStackProvider, ticks: Int = 0) : this(provider::stack, ticks)

    val stack: ItemStack get() = stackProvider()
}

class FrameTickRateProvider private constructor(
    private val provider: (AnimatedFrame) -> Int = { it.transitionTicks }
) {
    companion object {
        fun <E : Number> of(value: E) = FrameTickRateProvider { value.toInt() }
        fun <E : Number> of(property: Property<E>) = FrameTickRateProvider { property.get().toInt() }
    }

    fun getTransitionTicks(frame: AnimatedFrame): Int = provider(frame)
}
