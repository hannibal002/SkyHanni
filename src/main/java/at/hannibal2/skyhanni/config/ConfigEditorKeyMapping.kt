package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.utils.InputCode

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class ConfigEditorKeyMapping(
    val defaultKey: InputCode = UNKNOWN,
)
