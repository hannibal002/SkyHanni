package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.utils.InputCode

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigEditorKeymapping(val defaultKey: InputCode = UNKNOWN)
