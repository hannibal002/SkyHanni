package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.garden.sensitivity.MouseSensitivityManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MouseSensitivityHook {
    fun remapSensitivity(original: Float): Float {
        val actualSensitivity = (original - 0.2f) / 0.6f

        return MouseSensitivityManager.getSensitivity(actualSensitivity) * 0.6f + 0.2f
    }
}
