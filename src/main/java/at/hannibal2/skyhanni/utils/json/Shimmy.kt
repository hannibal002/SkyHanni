package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.ReflectionUtils.getPrivateField
import at.hannibal2.skyhanni.utils.ReflectionUtils.getPrivateFieldValue
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.github.notenoughupdates.moulconfig.observer.Property
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

// Copied (+ adapted to Kotlin) from NEU
/**
 * [type] is the type used for json (de)serialization. It is usually the generic type of [reflectField], except when
 * this shimmy points inside a moulconfig [Property], where the field type is erased to [Object] and the real type is
 * recovered from the property declaration instead.
 */
class Shimmy private constructor(val source: Any, val reflectField: Field, val type: Type) {
    val clazz: Class<*> = reflectField.type
    var value: Any?
        get() = reflectField.get(source)
        set(v) = reflectField.set(source, v)

    fun getJson(): JsonElement = ConfigManager.gson.toJsonTree(value, type)
    fun setJson(element: JsonElement) {
        val newValue = ConfigManager.gson.fromJson<Any?>(element, type)
        // Gson silently returns null for unknown enum constants, which would crash whoever reads the field later
        require(newValue != null || element.isJsonNull) {
            "Could not deserialize $element into ${TypeToken.get(type).rawType.name} for field ${reflectField.name}"
        }
        value = newValue
    }

    companion object {
        private fun traverse(source: Any?, fieldName: String): Any? =
            runCatching { source?.getPrivateFieldValue(fieldName) }.getOrNull()

        private fun field(source: Any, fieldName: String): Field? =
            runCatching { source.getPrivateField(fieldName) }.getOrNull()

        operator fun invoke(source: Any?, path: List<String>): Shimmy? {
            if (path.isEmpty()) return null
            val parent = path.dropLast(1).fold(source) { obj, part -> traverse(obj, part) } ?: return null
            val field = field(parent, path.last()) ?: return null
            val shimmy = Shimmy(parent, field, field.genericType)
            if (shimmy.clazz != Property::class.java) return shimmy
            val propertySource = traverse(parent, path.last()) ?: return shimmy
            val valueField = field(propertySource, "value") ?: return shimmy
            val propertyType = (field.genericType as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
            return Shimmy(propertySource, valueField, propertyType ?: valueField.genericType)
        }
    }
}
