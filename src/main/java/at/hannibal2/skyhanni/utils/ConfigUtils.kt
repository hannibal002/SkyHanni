package at.hannibal2.skyhanni.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlin.reflect.full.isSubclassOf

object ConfigUtils {

    /**
     * Migrates an ArrayList of enums to a JsonArray of enum names. The new enum class should implement LegacyList and have a getter for LegacyId
     *
     * @param element The JsonElement to migrate
     * @param enumClass The enum class to migrate to
     * @return The migrated JsonElement
     */
    fun <T : Enum<T>> migrateArrayListToJsonEnumArray(element: JsonElement, enumClass: Class<T>): JsonElement {
        require(element is JsonArray) { "Expected a JsonArray but got ${element.javaClass.simpleName}" }

        val kClass = enumClass.kotlin
        require(kClass.isSubclassOf(Enum::class)) { "Provided class is not an enum class" }

        // Getting the method from the enum class
        val getLegacyIdMethod = try {
            enumClass.getMethod("getLegacyId")
        } catch (e: NoSuchMethodException) {
            throw IllegalArgumentException("Enum class does not have a 'getLegacyId' method", e)
        }

        val migratedArray = element.mapNotNull { jsonElement ->
            val index = jsonElement.asInt
            enumClass.enumConstants?.firstOrNull {
                val legacyId = getLegacyIdMethod.invoke(it) as Int
                legacyId == index
            }?.name
        }.map { JsonPrimitive(it) }

        return JsonArray().apply {
            migratedArray.forEach { add(it) }
        }
    }

}
