package at.hannibal2.skyhanni.utils

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.Reader
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

inline fun <reified T : Any> Gson.fromJson(string: String): T = this.fromJson(string, typeOf<T>().javaType)

inline fun <reified T : Any> Gson.fromJson(jsonElement: JsonElement): T =
    this.fromJson(jsonElement, typeOf<T>().javaType)

inline fun <reified T : Any> Gson.fromJson(reader: Reader): T = this.fromJson(reader, typeOf<T>().javaType)

/**
 * Straight forward deep copy. This is included in gson as well, but different versions have it exposed privately instead of publicly,
 * so this reimplementation is here as an always public alternative.
 */
fun JsonElement.shDeepCopy(): JsonElement {
    return when (this) {
        is JsonObject -> JsonObject().also {
            for (entry in this.entrySet())
                it.add(entry.key, entry.value.shDeepCopy())
        }

        is JsonArray -> JsonArray().also {
            for (entry in this) {
                it.add(entry.shDeepCopy())
            }
        }

        else -> this
    }
}
