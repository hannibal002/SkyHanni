package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.config.ConfigManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Reader
import java.nio.charset.StandardCharsets
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

inline fun <reified T : Any> Gson.fromJson(string: String): T = this.fromJson(string, typeOf<T>().javaType)

inline fun <reified T : Any> Gson.fromJson(jsonElement: JsonElement): T =
    this.fromJson(jsonElement, typeOf<T>().javaType)

inline fun <reified T : Any> Gson.fromJson(reader: Reader): T = this.fromJson(reader, typeOf<T>().javaType)

fun File.getJson(gson: Gson = ConfigManager.gson): JsonElement? = runCatching {
    BufferedReader(
        InputStreamReader(
            FileInputStream(this),
            StandardCharsets.UTF_8,
        ),
    ).use { gson.fromJson(it, JsonElement::class.java) }
}.getOrNull()

fun File.writeJson(json: JsonElement, gson: Gson = ConfigManager.gson): Boolean = runCatching {
    if (!this.exists()) this.createNewFile()
    BufferedWriter(
        OutputStreamWriter(
            FileOutputStream(this),
            StandardCharsets.UTF_8,
        )
    ).use { it.write(gson.toJson(json)) }
    true
}.getOrElse { return false }

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

fun Iterable<JsonElement>.toJsonArray(): JsonArray = JsonArray().also {
    for (jsonElement in this) {
        it.add(jsonElement)
    }
}

val JsonPrimitive.asIntOrNull get() = takeIf { it.isNumber }?.asInt
