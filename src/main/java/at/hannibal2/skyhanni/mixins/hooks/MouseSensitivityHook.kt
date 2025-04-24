package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MouseSensitivityHook {
    private val config get() = SkyHanniMod.feature.garden.sensitivityReducer
    var state: MouseSensitivityState = MouseSensitivityState.DEFAULT
        private set

    private var lastIn: Float = Float.NaN
    private var lastOut: Float = Float.NaN

    private fun getMouseSensitivity(original: Float): Float {
        if (original != lastIn) {
            lastIn = original
            lastOut = state.apply(original)
        }

        return lastOut
    }

    fun getMouseSensitivityWithWeirdMath(original: Float): Float {
        val actualSensitivity = (original - 0.2f) / 0.6f

        return getMouseSensitivity(actualSensitivity) * 0.6f + 0.2f
    }

    fun setState(newState: MouseSensitivityState) {
        state = newState

        lastIn = Float.NaN
        lastOut = Float.NaN
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Mouse Sensitivity")

        if (state == MouseSensitivityState.DEFAULT) {
            event.addIrrelevant("not enabled")
            return
        }

        event.addData {
            add("current state: $state")
        }
    }

    enum class MouseSensitivityState(
        private val transform: ((Float) -> Float),
    ) {
        DEFAULT({ it }),
        LOCKED({ _ -> -1f / 3f }),
        AUTO_REDUCED(
            {
                ((it + 1f / 3f) / config.reducingFactor.get()) - 1f / 3f
            },
        ),
        MANUAL_REDUCED(
            {
                ((it + 1f / 3f) / config.reducingFactor.get()) - 1f / 3f
            },
        ),
        ;

        fun apply(original: Float): Float = transform(original)
    }
}
