package de.hype.bingonet.shared.objects.json

import com.google.gson.JsonElement
import de.hype.bingonet.shared.objects.json.ApiJson.Companion.of

class ApiJsonElement(var element: JsonElement?) {
    var isOffPath: Boolean = false

    init {
        if (element == null) isOffPath = true
    }

    val asObject: ApiJson
        get() = of(element?.asJsonObject)

    fun getString(def: String): String {
        if (isOffPath) return def
        val element = element
        if (element == null || element.isJsonNull) return def
        return element.asString
    }

    @JvmName("getNullableString")
    fun getString(def: String?): String? {
        if (isOffPath) return def
        val element = element
        if (element == null || element.isJsonNull) return def
        return element.asString
    }

    fun getLong(def: Long): Long {
        if (isOffPath) return def
        val element = element
        if (element == null || element.isJsonNull) return def
        return element.asLong
    }

    @JvmName("getNullableLong")
    fun getLong(def: Long?): Long? {
        if (isOffPath) return def
        val element = element
        if (element == null || element.isJsonNull) return def
        return element.asLong
    }

    fun getBoolean(def: Boolean): Boolean {
        if (isOffPath) return def
        val element = element
        if (element == null || element.isJsonNull) return def
        return element.asBoolean
    }

    @JvmName("getNullableBoolean")
    fun getBoolean(def: Boolean?): Boolean? {
        if (isOffPath) return def
        val element = element
        if (element == null || element.isJsonNull) return def
        return element.asBoolean
    }

    fun getInt(def: Int): Int {
        if (isOffPath) return def
        val element = element
        if (element == null || element.isJsonNull) return def
        return element.asInt
    }

    @JvmName("getNullableInt")
    fun getInt(def: Int?): Int? {
        if (isOffPath) return def
        val element = element
        if (element == null || element.isJsonNull) return def
        return element.asInt
    }

    fun getString(): String {
        return getString("")
    }

    fun getInt(): Int {
        return getInt(0)
    }

    fun getLong(): Long {
        return getLong(0L)
    }

    fun getBoolean(): Boolean {
        return getBoolean(false)
    }
}
