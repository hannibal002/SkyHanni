package at.hannibal2.skyhanni.features.garden.sensitivity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.features.garden.sensitivity.SensitivityReducer.REDUCING_FACTOR_HARD_BOUNDS
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MouseSensitivityManager {

    private val config get() = SkyHanniMod.feature.garden.sensitivityReducer

    private var lastIn = Float.NaN
    private var lastOut = Float.NaN

    var state: SensitivityState = SensitivityState.UNCHANGED
        set(value) {
            field = value
            destroyCache()
        }

    private fun reduceTransform(f: Float): Float {
        val reducingFactor = config.reducingFactor.get().coerceIn(REDUCING_FACTOR_HARD_BOUNDS)
        return ((f + 1f / 3f) / reducingFactor) - 1f / 3f
    }

    fun getSensitivity(original: Float): Float {
        if (original != lastIn) {
            lastIn = original
            lastOut = state.apply(original)
        }

        return lastOut
    }

    fun destroyCache() {
        lastIn = Float.NaN
        lastOut = Float.NaN
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Mouse Sensitivity")

        if (SensitivityState.UNCHANGED.isActive()) {
            event.addIrrelevant("not enabled")
            return
        }

        event.addData {
            add("current state: $state")
        }
    }

    enum class SensitivityState(
        private val transform: ((Float) -> Float),
    ) {
        UNCHANGED({ it }),
        LOCKED({ _ -> -1f / 3f }),
        AUTO_REDUCED(::reduceTransform),
        MANUAL_REDUCED(::reduceTransform),
        ;

        fun apply(original: Float): Float = transform(original)
        fun isActive(): Boolean = this == state
    }
}
