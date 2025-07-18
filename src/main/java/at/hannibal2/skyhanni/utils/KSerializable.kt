package at.hannibal2.skyhanni.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.javaType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.typeOf
import com.google.gson.internal.`$Gson$Types` as InternalGsonTypes

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KSerializable

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ExtraData

class KotlinTypeAdapterFactory : TypeAdapterFactory {

    private data class ParamInfo(
        val param: KParameter,
        val name: String,
        val adapter: TypeAdapter<Any?>,
        val prop: KProperty1<Any, Any?>,
    )

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        val kClass = typeToken.rawType.kotlin as KClass<T>

        // only data‐classes annotated @KSerializable
        if (!kClass.isData || kClass.findAnnotation<KSerializable>() == null) return null
        val ctor = kClass.primaryConstructor ?: return null

        // pick off any @ExtraData param (must be a Map<String,JsonElement>)
        val extraParam = ctor.parameters.find {
            it.findAnnotation<ExtraData>() != null
                && typeOf<Map<String, JsonElement>>().isSubtypeOf(it.type)
        }

        // build info for every real constructor parameter
        val infos = ctor.parameters.filter { it.findAnnotation<ExtraData>() == null }.map { param ->
            // find the matching Kotlin property
            val memberProp = kClass.memberProperties.find { it.name == param.name } ?: return null
            val prop = memberProp.also { it.isAccessible = true } as KProperty1<Any, Any?>

            // determine JSON name (@SerializedName on param, prop, field or getter, fallback to param name)
            val name = param.findAnnotation<SerializedName>()?.value
                ?: prop.findAnnotation<SerializedName>()?.value
                ?: prop.javaField?.getAnnotation(SerializedName::class.java)?.value
                ?: prop.javaGetter?.getAnnotation(SerializedName::class.java)?.value
                ?: param.name
                ?: error("Could not determine JSON name for parameter '${param.name}' in class '${kClass.simpleName}'")

            // figure out the runtime type to ask GSON for
            val javaType = prop.javaField?.genericType
                ?: prop.javaGetter?.genericReturnType
                ?: param.type.javaType
            val resolved = InternalGsonTypes.resolve(typeToken.type, typeToken.rawType, javaType)
                ?: error("Could not resolve type for parameter '${param.name}' in class '${kClass.simpleName}'")
            val adapter = gson.getAdapter(TypeToken.get(resolved)) as TypeAdapter<Any?>

            ParamInfo(param, name, adapter, prop)
        }

        val infosByName = infos.associateBy { it.name }

        // and a JsonElement adapter for any extra fields
        val jsonElementAdapter = gson.getAdapter(JsonElement::class.java)

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                if (value == null) {
                    out.nullValue(); return
                }
                out.beginObject()
                for (info in infos) {
                    out.name(info.name)
                    info.adapter.write(out, info.prop.get(value))
                }
                if (extraParam != null) {
                    val map = value::class
                        .memberProperties
                        .find { it.name == extraParam.name }
                        ?.getter?.call(value) as? Map<String, JsonElement>
                        ?: error("ExtraData parameter must be a Map<String, JsonElement>")
                    for ((extraName, extraValue) in map) {
                        out.name(extraName)
                        jsonElementAdapter.write(out, extraValue)
                    }
                }
                out.endObject()
            }

            override fun read(reader: JsonReader): T? {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    return null
                }
                reader.beginObject()
                val args = mutableMapOf<KParameter, Any?>()
                val extraData = mutableMapOf<String, JsonElement>()
                while (reader.peek() != JsonToken.END_OBJECT) {
                    val name = reader.nextName()
                    val paramData = infosByName[name]
                    if (paramData != null) args[paramData.param] = paramData.adapter.read(reader)
                    else extraData[name] = jsonElementAdapter.read(reader)
                }
                reader.endObject()

                // stash unknowns into the @ExtraData parameter, if requested
                if (extraParam != null) args[extraParam] = extraData
                // call the Kotlin ctor (will honor defaults)
                return ctor.callBy(args)
            }
        }.nullSafe()
    }
}
