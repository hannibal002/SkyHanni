package at.hannibal2.skyhanni.config.core.elements


@Retention(RUNTIME)
@Target(FIELD)
annotation class ConfigEditorKeyMap(
    val defaultKey: Int,
    val displayName: String = "",
)
