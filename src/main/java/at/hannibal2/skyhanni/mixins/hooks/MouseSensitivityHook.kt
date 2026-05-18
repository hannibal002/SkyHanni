package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.garden.sensitivity.MouseSensitivityManager

object MouseSensitivityHook {

    @JvmStatic
    fun remapSensitivity(original: Float): Float {
        val actualSensitivity = (original - 0.2f) / 0.6f
        val mapped = MouseSensitivityManager.getSensitivity(actualSensitivity) * 0.6f + 0.2f
        check(mapped.isFinite()) {
            "Mouse sensitivity is $mapped (expected finite value)"
        }
        return mapped
    }
}
