package at.hannibal2.skyhanni.utils.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

open class SimpleTypedTypeAdapter<P, T>(
    private val jsonRead: JsonReader.() -> P,
    private val jsonWrite: JsonWriter.(P) -> Unit,
    val serializer: T.() -> P,
    val deserializer: P.() -> T,
) : TypeAdapter<T>() {

    override fun write(writer: JsonWriter, value: T?) {
        if (value == null) writer.nullValue()
        else writer.jsonWrite(serializer(value))
    }

    override fun read(reader: JsonReader): T? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return deserializer(reader.jsonRead())
    }
}

class SimpleStringTypeAdapter<T>(
    serializer: T.() -> String,
    deserializer: String.() -> T,
) : SimpleTypedTypeAdapter<String, T>(JsonReader::nextString, JsonWriter::value, serializer, deserializer) {

    companion object {
        val enumReplacementMap = mutableMapOf<Enum<*>, String>()

        inline fun <reified T : Enum<T>> forEnum() = SimpleStringTypeAdapter<T>(
            serializer = { name },
            deserializer = { enumValueOf(replace(" ", "_").uppercase()) },
        )

        inline fun <reified T : Enum<T>> forEnum(defaultValue: T) = SimpleStringTypeAdapter<T>(
            serializer = { if (this == defaultValue) enumReplacementMap[defaultValue] ?: name else name },
            deserializer = {
                try {
                    enumValueOf(replace(" ", "_").uppercase())
                } catch (_: IllegalArgumentException) {
                    enumReplacementMap[defaultValue] = this
                    defaultValue
                }
            },
        )
    }
}

class SimpleLongTypeAdapter<T>(
    serializer: T.() -> Long,
    deserializer: Long.() -> T,
) : SimpleTypedTypeAdapter<Long, T>(JsonReader::nextLong, JsonWriter::value, serializer, deserializer)
