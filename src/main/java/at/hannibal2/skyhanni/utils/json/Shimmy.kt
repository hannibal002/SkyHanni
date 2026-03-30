package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.ReflectionUtils.getPrivateField
import at.hannibal2.skyhanni.utils.ReflectionUtils.getPrivateFieldValue
import com.google.gson.JsonElement
import io.github.notenoughupdates.moulconfig.observer.Property
import java.lang.reflect.Field

// Copied (+ adapted to Kotlin) from NEU
class Shimmy private constructor(val source: Any, val reflectField: Field) {
    val clazz: Class<*> = reflectField.type
    var value: Any?
        get() = reflectField.get(source)
        set(v) = reflectField.set(source, v)

    fun getJson(): JsonElement = ConfigManager.gson.toJsonTree(value)
    fun setJson(element: JsonElement) {
        value = ConfigManager.gson.fromJson(element, clazz)
    }

    companion object {
        private fun traverse(source: Any?, fieldName: String): Any? =
            runCatching { source?.getPrivateFieldValue(fieldName) }.getOrNull()

        operator fun invoke(source: Any?, path: List<String>): Shimmy? {
            if (path.isEmpty()) return null
            val parent = path.dropLast(1).fold(source) { obj, part -> traverse(obj, part) } ?: return null
            return try {
                val field = parent.getPrivateField(path.last())
                val shimmy = Shimmy(parent, field)
                if (shimmy.clazz != Property::class.java) return shimmy
                val propertySource = traverse(parent, path.last()) ?: return shimmy
                invoke(propertySource, listOf("value")) ?: shimmy
            } catch (e: NoSuchFieldException) {
                null
            }
        }
    }
}
