package at.hannibal2.skyhanni.mixins.hooks

object MouseSensitivityHook {
    fun mouseSensitivityOverride(original: Float): Float {
        return original
    }
}
