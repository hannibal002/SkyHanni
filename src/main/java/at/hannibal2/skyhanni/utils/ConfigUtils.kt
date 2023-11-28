package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.HasLegacyId
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

object ConfigUtils {

    /**
     * Migrates an ArrayList of enums to a JsonArray of enum names. The new enum class should implement LegacyList and have a getter for LegacyId
     *
     * @param element The JsonElement to migrate
     * @param enumClass The enum class to migrate to
     * @return The migrated JsonElement
     */
    fun <T> migrateArrayListToJsonEnumArray(element: JsonElement, enumClass: Class<T>): JsonElement
        where T : Enum<T>, T : HasLegacyId {
        require(element is JsonArray) { "Expected a JsonArray but got ${element.javaClass.simpleName}" }

        // An array of enum constants that are to be migrated
        val migratedArray = element.mapNotNull { jsonElement ->
            val index = jsonElement.asInt
            getEnumConstantFromLegacyId(index, enumClass)?.name
        }.map { JsonPrimitive(it) }

        // Return a JsonArray of the migrated enum constants
        return JsonArray().apply {
            migratedArray.forEach { add(it) }
        }
    }

    /**
     * Gets an enum constant from a legacy id
     * @param legacyId The legacy id to get the enum constant from
     * @param enumClass The enum class to get the enum constant from
     * @return The enum constant, or null if not found
     */
    private fun <T> getEnumConstantFromLegacyId(
        legacyId: Int,
        enumClass: Class<T>
    ): T? where T : Enum<T>, T : HasLegacyId {
        for (enumConstant in enumClass.getEnumConstants()) {
            if (enumConstant.legacyId == legacyId) return enumConstant
        }
        return null
    }
}
