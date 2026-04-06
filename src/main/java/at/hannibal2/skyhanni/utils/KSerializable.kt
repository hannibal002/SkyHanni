@file:OptIn(ExperimentalStdlibApi::class)

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
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.javaType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf
import com.google.gson.internal.`$Gson$Types` as InternalGsonTypes

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KSerializable

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ExtraData

class KotlinTypeAdapterFactory : TypeAdapterFactory {

    internal data class ParameterInfo(
        val param: KParameter,
        val adapter: TypeAdapter<Any?>,
        val name: String,
        val field: KProperty1<Any, Any?>,
    )

    /**
     * Resolves the backing property for [param] in [kotlinClass].
     *
     * Tries [KClass.memberProperties] first (covers all visible inherited properties),
     * then falls back to searching [KClass.allSuperclasses] declared properties to handle
     * the case where the property is private in a superclass and therefore not visible
     * from the child's [KClass.memberProperties].
     *
     * @param param The constructor parameter to resolve a backing property for.
     * @param kotlinClass The data class being deserialized.
     */
    @Suppress("UNCHECKED_CAST")
    private fun resolveField(param: KParameter, kotlinClass: KClass<*>): KProperty1<Any, Any?>? {
        val found = kotlinClass.memberProperties.singleOrNull { it.name == param.name }
            ?: kotlinClass.allSuperclasses
                .flatMap { it.declaredMemberProperties }
                .find { it.name == param.name }
        return found as? KProperty1<Any, Any?>
    }

    /**
     * Finds the constructor parameter annotated with [ExtraData] and its corresponding property,
     * if present. The parameter must be of type [MutableMap]<[String], [JsonElement]>.
     *
     * @param kotlinClass The data class being deserialized.
     * @param primaryConstructorParams All parameters of the primary constructor.
     */
    private fun buildExtraDataParam(
        kotlinClass: KClass<*>,
        primaryConstructorParams: List<KParameter>,
    ): Pair<KParameter, KProperty1<Any, Map<String, JsonElement>>>? {
        val param = primaryConstructorParams.find {
            it.findAnnotation<ExtraData>() != null &&
                typeOf<MutableMap<String, JsonElement>>().isSubtypeOf(it.type)
        } ?: return null
        @Suppress("UNCHECKED_CAST")
        val property = kotlinClass.memberProperties.find {
            it.name == param.name && it.returnType.isSubtypeOf(typeOf<Map<String, JsonElement>>())
        } as? KProperty1<Any, Map<String, JsonElement>> ?: return null
        property.isAccessible = true
        return param to property
    }

    /**
     * Builds the map of JSON key to [ParameterInfo] used by both the reader and writer.
     * Resolves each parameter's backing property via [resolveField], determines the JSON key
     * from [SerializedName] (falling back to the parameter name), and selects the appropriate
     * [TypeAdapter] from [gson].
     *
     * @param kotlinClass The data class being serialized/deserialized.
     * @param gson The Gson instance to source type adapters from.
     * @param type The [TypeToken] for [kotlinClass], used for generic type resolution.
     * @param params The non-[ExtraData] primary constructor parameters to process.
     */
    private fun <T : Any> buildParameterInfos(
        kotlinClass: KClass<T>,
        gson: Gson,
        type: TypeToken<T>,
        params: List<KParameter>,
    ): Map<String, ParameterInfo> = params.mapNotNull { param ->
        val field = resolveField(param, kotlinClass) ?: return@mapNotNull null
        runCatching { field.isAccessible = true }.getOrNull() ?: return@mapNotNull null
        val kType = field.returnType
        val name = param.findAnnotation<SerializedName>()?.value
            ?: field.findAnnotation<SerializedName>()?.value
            ?: field.javaField?.getAnnotation(SerializedName::class.java)?.value
            ?: param.name!!
        val javaTypeForAdapter = if (kType.jvmErasure.java.isAnnotationPresent(JvmInline::class.java)) {
            kType.jvmErasure.java
        } else {
            InternalGsonTypes.resolve(type.type, type.rawType, kType.javaType)
        }
        @Suppress("UNCHECKED_CAST")
        val adapter = gson.getAdapter(TypeToken.get(javaTypeForAdapter)) as TypeAdapter<Any?>
        ParameterInfo(param, adapter, name, field)
    }.associateBy { it.name }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val kotlinClass = (type.rawType.kotlin as KClass<T>).takeIf {
            it.findAnnotation<KSerializable>() != null && it.isData
        } ?: return null

        val primaryConstructor = kotlinClass.primaryConstructor?.apply {
            isAccessible = true
        } ?: return null

        val params = primaryConstructor.parameters.filter { it.findAnnotation<ExtraData>() == null }
        val extraDataParam = buildExtraDataParam(kotlinClass, primaryConstructor.parameters)
        val parameterInfos = buildParameterInfos(kotlinClass, gson, type, params)
        val jsonElementAdapter = gson.getAdapter(JsonElement::class.java)

        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                if (value == null) {
                    out.nullValue()
                    return
                }
                val prevSerializeNulls = out.serializeNulls
                out.serializeNulls = true
                out.beginObject()
                for ((name, paramInfo) in parameterInfos) {
                    out.name(name)
                    paramInfo.adapter.write(out, paramInfo.field.get(value))
                }
                if (extraDataParam != null) {
                    for ((extraName, extraValue) in extraDataParam.second.get(value)) {
                        out.name(extraName)
                        jsonElementAdapter.write(out, extraValue)
                    }
                }
                out.endObject()
                out.serializeNulls = prevSerializeNulls
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
                    val paramData = parameterInfos[name]
                    if (paramData == null) {
                        extraData[name] = jsonElementAdapter.read(reader)
                        continue
                    }
                    args[paramData.param] = paramData.adapter.read(reader)
                }
                reader.endObject()
                if (extraDataParam != null) {
                    args[extraDataParam.first] = extraData
                }
                try {
                    return primaryConstructor.callBy(args)
                } catch (e: IllegalArgumentException) {
                    val errorString = buildString {
                        appendLine("Failed to invoke constructor for ${kotlinClass.simpleName}")
                        appendLine("  Reason: ${e.message}")
                        args.forEach { (param, value) ->
                            appendLine("  - ${param.name} : expected=${param.type} value=$value actualType=${value?.javaClass}")
                        }
                    }
                    System.err.println(errorString)
                    throw e
                }
            }
        }
    }
}
