package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.SkyHanniMod
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

object SkippingTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        return SafeTypeAdapter(gson.getDelegateAdapter(this, type))
    }

    private class SafeTypeAdapter<T>(val parent: TypeAdapter<T>) : TypeAdapter<T>() {
        override fun write(writer: JsonWriter, value: T) {
            parent.write(writer, value)
        }

        override fun read(reader: JsonReader): T? {
            return try {
                parent.read(reader)
            } catch (e: Exception) {
                SkyHanniMod.logger.warn("Failed to read value from JSON, skipping", e)
                reader.skipValue()
                null
            }
        }
    }
}
