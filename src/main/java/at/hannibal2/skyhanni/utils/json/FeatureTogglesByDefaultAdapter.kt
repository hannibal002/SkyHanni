package at.hannibal2.skyhanni.utils.json

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ReflectionUtils.getDeclaredFieldOrNull
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.bind.JsonTreeReader
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Field
import java.lang.reflect.Type
import com.google.gson.internal.`$Gson$Types` as InternalGsonTypes

object FeatureTogglesByDefaultAdapter : TypeAdapterFactory {
    fun <T> getType(typeToken: TypeToken<T>, field: Field): Type {
        return InternalGsonTypes.resolve(typeToken.type, typeToken.rawType, field.genericType)
    }

    private fun Class<*>.getFieldInHierarchyOrNull(name: String): Field? {
        var clazz: Class<*>? = this
        while (clazz != null) {
            val field = clazz.getDeclaredFieldOrNull(name)
            if (field != null) return field
            clazz = clazz.superclass
        }
        return null
    }

    class Adapter<T>(
        val originalWrite: TypeAdapter<T>,
        val clazz: Class<T>,
        val gson: Gson,
        val type: TypeToken<T>,
    ) : TypeAdapter<T>() {
        override fun write(out: JsonWriter, value: T) {
            originalWrite.write(out, value)
        }

        override fun read(reader: JsonReader): T {
            reader.beginObject()
            // Create a default initialized instance
            val obj = clazz.getDeclaredConstructor().newInstance()

            // Overwrite the default with true (or false) for feature toggles
            for (field in clazz.fields) {
                val featureToggle = field.getAnnotation(FeatureToggle::class.java)
                val adapt = gson.getAdapter(TypeToken.get(getType(type, field)))
                fun JsonElement.adaptRead() = field.set(obj, adapt.read(JsonTreeReader(this)))

                if (featureToggle != null) JsonPrimitive(featureToggle.trueIsEnabled).adaptRead()
                if (adapt is Adapter) JsonObject().adaptRead()
            }

            // Read the actual JSON Object
            while (reader.peek() != JsonToken.END_OBJECT) {
                // IllegalStateException: Expected NAME but was BOOLEAN
                if (reader.peek() != JsonToken.NAME) {
                    reader.skipValue()
                    continue
                }
                val fieldName = reader.nextName()
                val field = clazz.getFieldInHierarchyOrNull(fieldName)
                if (field == null) {
                    reader.skipValue()
                    println("field is in config file, but not in object file: $fieldName")
                    continue
                }
                val fieldType = gson.getAdapter(TypeToken.get(getType(type, field)))
                // Read the field data
                val data = fieldType.read(reader)
                // Set the field or override the feature toggle with the saved data,
                // leaving only the unset feature toggles to deviate from their defaults
                field.set(obj, data)
            }

            reader.endObject()
            return obj
        }
    }

    override fun <T> create(gson: Gson?, type: TypeToken<T>): TypeAdapter<T>? {
        require(gson != null)
        val rawType = type.rawType?.takeIf {
            it.fields.any { field ->
                field.isAnnotationPresent(FeatureToggle::class.java) ||
                    gson.getAdapter(TypeToken.get(getType(type, field))) is Adapter
            }
        } ?: return null

        val originalWrite = gson.getDelegateAdapter(this, type)
        @Suppress("UNCHECKED_CAST")
        return Adapter(originalWrite, rawType as Class<T>, gson, type)
    }
}
