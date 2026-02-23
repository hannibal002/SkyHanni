package at.hannibal2.skyhanni.utils.renderables.animated

import io.github.notenoughupdates.moulconfig.observer.Property

class FrameTickRateProvider private constructor(private val provider: () -> Double) {
    operator fun invoke(): Double = provider()
    companion object {
        fun <E : Number> of(value: E) = FrameTickRateProvider { value.toDouble() }
        fun of(provider: () -> Double) = FrameTickRateProvider(provider)
        fun <E : Number> of(property: Property<E>) = FrameTickRateProvider { property.get().toDouble() }
        fun perFrame() = FrameTickRateProvider { -1.0 }
    }
}
