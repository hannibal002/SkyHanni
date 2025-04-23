package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.misc.MouseSensitivityManager

object MouseSensitivityHook {
    fun mouseSensitivityOverride(original: Float): Float {
        return MouseSensitivityManager.mouseSensitivityState.apply(original)
    }
}
