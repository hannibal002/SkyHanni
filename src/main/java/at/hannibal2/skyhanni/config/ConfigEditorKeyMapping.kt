package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.utils.InputKeys
import com.mojang.blaze3d.platform.InputConstants

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigEditorKeyMapping(
    val defaultKey: Int = InputKeys.KEY_UNKNOWN,
    //~ if < 26.3 'KEYBOARD' -> 'KEYSYM'
    val type: InputConstants.Type = KEYBOARD,
)
