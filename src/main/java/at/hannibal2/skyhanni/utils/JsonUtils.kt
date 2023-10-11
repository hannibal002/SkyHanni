package at.hannibal2.skyhanni.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.Reader
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

inline fun <reified T : Any> Gson.fromJson(string: String): T = this.fromJson(string, typeOf<T>().javaType)

inline fun <reified T : Any> Gson.fromJson(jsonElement: JsonElement): T =
    this.fromJson(jsonElement, typeOf<T>().javaType)

inline fun <reified T : Any> Gson.fromJson(reader: Reader): T = this.fromJson(reader, typeOf<T>().javaType)
