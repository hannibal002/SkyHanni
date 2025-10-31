package at.hannibal2.hanni.mixins.hooks

import at.hannibal2.hanni.features.garden.sensitivity.MouseSensitivityManager

object MouseSensitivityHook {
    fun remapSensitivity(original: Float): Float {
        val actualSensitivity = (original - 0.2f) / 0.6f

        return MouseSensitivityManager.getSensitivity(actualSensitivity) * 0.6f + 0.2f
    }
}
