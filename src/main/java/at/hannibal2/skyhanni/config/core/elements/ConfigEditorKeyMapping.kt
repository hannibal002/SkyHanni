package at.hannibal2.skyhanni.config.core.elements

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigEditorKeyMapping(
    val defaultKey: String,
)
