package at.hannibal2.skyhanni.utils

object EnumUtils {

    inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String): T? {
        val enums = enumValues<T>()
        return enums.firstOrNull { it.name == name }
    }

    inline fun <reified T : Enum<T>> enumValueOf(name: String) =
        enumValueOfOrNull<T>(name) ?: error("Unknown enum constant for ${enumValues<T>().first().name.javaClass.simpleName}: '$name'")

    inline fun <reified T : Enum<T>> enumJoinToPattern(noinline transform: (T) -> CharSequence = { it.name }) =
        enumValues<T>().joinToString("|", transform = transform)

    inline fun <reified T : Enum<T>> T.isAnyOf(vararg array: T): Boolean = array.contains(this)

}
