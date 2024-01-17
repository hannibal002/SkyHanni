package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.HasLegacyId
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

object ConfigUtils {

    /**
     * Migrates an Int ArrayList to an Enum ArrayList.
     * The new enum class should implement HasLegacyId and have a getter for LegacyId
     *
     * @param element The JsonElement to migrate
     * @param enumClass The enum class to migrate to
     * @return The migrated JsonElement
     */
    fun <T> migrateIntArrayListToEnumArrayList(element: JsonElement, enumClass: Class<T>): JsonElement
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
    ): T? where T : Enum<T>, T : HasLegacyId = enumClass.getEnumConstants().firstOrNull { it.legacyId == legacyId }

    /**
     * Migrates an Int to an Enum Constant.
     * The new enum class should implement HasLegacyId and have a getter for LegacyId
     *
     * @param element The JsonElement to migrate
     * @param enumClass The enum class to migrate to
     * @return The migrated JsonElement
     */
    fun <T> migrateIntToEnum(element: JsonElement, enumClass: Class<T>): JsonElement
        where T : Enum<T>, T : HasLegacyId {
        require(element is JsonPrimitive) { "Expected a JsonPrimitive but got ${element.javaClass.simpleName}" }
        return JsonPrimitive(getEnumConstantFromLegacyId(element.asInt, enumClass)?.name)
    }
}
