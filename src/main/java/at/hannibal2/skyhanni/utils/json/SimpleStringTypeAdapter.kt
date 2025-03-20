package at.hannibal2.skyhanni.utils.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class SimpleStringTypeAdapter<T>(
    val serializer: T.() -> String,
    val deserializer: String.() -> T,
) : TypeAdapter<T>() {

    override fun write(writer: JsonWriter, value: T) {
        writer.value(serializer(value))
    }

    override fun read(reader: JsonReader): T {
        return deserializer(reader.nextString())
    }

    companion object {
        val enumReplacementMap = mutableMapOf<Enum<*>, String>()

        inline fun <reified T : Enum<T>> forEnum(): SimpleStringTypeAdapter<T> {
            return SimpleStringTypeAdapter(
                { name },
                { enumValueOf(this.replace(" ", "_").uppercase()) },
            )
        }

        inline fun <reified T : Enum<T>> forEnum(defaultValue: T): SimpleStringTypeAdapter<T> {
            return SimpleStringTypeAdapter(
                {
                    if (this == defaultValue) {
                        enumReplacementMap[defaultValue] ?: name
                    } else {
                        name
                    }
                },
                {
                    try {
                        enumValueOf(this.replace(" ", "_").uppercase())
                    } catch (e: IllegalArgumentException) {
                        enumReplacementMap[defaultValue] = this
                        println("setting enumReplacementMap[$defaultValue] = $this")
                        defaultValue
                    }
                },
            )
        }
    }
}
