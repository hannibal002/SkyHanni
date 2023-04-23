package at.hannibal2.skyhanni.config.migration

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigMigrationEvent
import at.hannibal2.skyhanni.utils.allAccessibleFields
import at.hannibal2.skyhanni.utils.nonGeneric
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.Expose
import io.github.moulberry.moulconfig.observer.Property
import net.minecraftforge.common.MinecraftForge
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


class MigratingConfigLoader {
    val logger = SkyHanniMod.getLogger("ConfigMigrator")
    val allFailures = mutableListOf<LoadResult.Failure>()

    val adapters = listOf(
        LoadingAdapter { path, hierarchy, type ->
            val ng = type.nonGeneric ?: return@LoadingAdapter LoadResult.Invalid
            if (!ng.isPrimitive) return@LoadingAdapter LoadResult.Invalid
            @Suppress("RemoveRedundantQualifierName")
            loadElement(
                path, hierarchy, mapOf(
                    java.lang.Integer.TYPE to Integer::class.java,
                    java.lang.Boolean.TYPE to Boolean::class.java,
                    java.lang.Short.TYPE to Short::class.java,
                    java.lang.Float.TYPE to Float::class.java,
                    java.lang.Double.TYPE to Double::class.java,
                    java.lang.Long.TYPE to Long::class.java,
                    java.lang.Byte.TYPE to Byte::class.java,
                    java.lang.Character.TYPE to Character::class.java,
                )[ng]!!
            )
        },
        directLoader { if (!it.asJsonPrimitive.isString) error("String expected, found $it") else LoadResult.Instance(it.asString) },
        directLoader { if (!it.asJsonPrimitive.isNumber) error("Number expected, found $it") else LoadResult.Instance(it.asInt) },
        directLoader { if (!it.asJsonPrimitive.isNumber) error("Number expected, found $it") else LoadResult.Instance(it.asFloat) },
        directLoader { if (!it.asJsonPrimitive.isNumber) error("Number expected, found $it") else LoadResult.Instance(it.asLong) },
        directLoader { if (!it.asJsonPrimitive.isNumber) error("Number expected, found $it") else LoadResult.Instance(it.asDouble) },
        directLoader { if (!it.asJsonPrimitive.isNumber) error("Number expected, found $it") else LoadResult.Instance(it.asNumber) },
        directLoader { if (!it.asJsonPrimitive.isNumber) error("Number expected, found $it") else LoadResult.Instance(it.asBigDecimal) },
        directLoader { if (!it.asJsonPrimitive.isNumber) error("Number expected, found $it") else LoadResult.Instance(it.asBigInteger) },
        directLoader {
            if (!it.asJsonPrimitive.isBoolean) error("Boolean expected, found $it") else LoadResult.Instance(
                it.asBoolean
            )
        },
        directLoader { if (!it.asJsonPrimitive.isNumber) error("Number expected, found $it") else LoadResult.Instance(it.asShort) },
        directLoader { if (!it.isJsonObject) error("JsonObject expected, found $it") else LoadResult.Instance(it.asJsonObject) },
        directLoader { if (!it.isJsonArray) error("JsonArray expected, found $it") else LoadResult.Instance(it.asJsonArray) },
        directLoader { if (!it.isJsonPrimitive) error("JsonPrimitive expected, found $it") else LoadResult.Instance(it.asJsonPrimitive) },
        directLoader { if (!it.isJsonNull) error("Null expected, found $it") else LoadResult.Instance(it as JsonNull) },
        directLoader { if (!it.asJsonPrimitive.isNumber) error("Number expected, found $it") else LoadResult.Instance(it.asByte) },
        directLoader {
            if (!it.asJsonPrimitive.isString || it.asString.length > 1) error("1-length String expected, found $it") else LoadResult.Instance(
                it.asCharacter
            )
        },
        LoadingAdapter { path, hierarchy, type ->
            val ng = type.nonGeneric ?: return@LoadingAdapter LoadResult.Invalid
            if (ng != List::class.java) return@LoadingAdapter LoadResult.Invalid
            type as ParameterizedType
            val childPath = ResolutionPath.IndirectChild("element", path)
            val builder = mutableListOf<Any?>()
            for (jsonElement in hierarchy.last()!!.asJsonArray) {
                val x = loadElement(childPath, hierarchy + jsonElement, type.actualTypeArguments[0])
                if (x is LoadResult.Instance) {
                    builder.add(x.instance)
                } else if (x is LoadResult.UseDefault) {
                    return@LoadingAdapter LoadResult.Failure(
                        RuntimeException("Cannot UseDefault for list element"),
                        childPath
                    )
                } else {
                    return@LoadingAdapter x
                }
            }
            return@LoadingAdapter LoadResult.Instance(builder)
        },
        LoadingAdapter { path, hierarchy, type ->
            val ng = type.nonGeneric ?: return@LoadingAdapter LoadResult.Invalid
            if (ng != Map::class.java) return@LoadingAdapter LoadResult.Invalid
            type as ParameterizedType
            val builder = mutableMapOf<Any?, Any?>()
            val keyPath = ResolutionPath.IndirectChild("key", path)
            val valuePath = ResolutionPath.IndirectChild("value", path)
            for ((key, value) in (hierarchy.last()!! as JsonObject).entrySet()) {
                val keyEl = loadElement(keyPath, hierarchy + JsonPrimitive(key), type.actualTypeArguments[0])
                if (keyEl !is LoadResult.Instance<*>) {
                    if (keyEl is LoadResult.UseDefault) {
                        return@LoadingAdapter LoadResult.Failure(
                            RuntimeException("Cannot UseDefault for map key"),
                            keyPath
                        )
                    }
                    return@LoadingAdapter keyEl
                }
                val valueEl = loadElement(valuePath, hierarchy + value, type.actualTypeArguments[0])
                if (valueEl !is LoadResult.Instance<*>) {
                    if (valueEl is LoadResult.UseDefault) {
                        return@LoadingAdapter LoadResult.Failure(
                            RuntimeException("Cannot UseDefault for map key"),
                            valuePath
                        )
                    }
                    return@LoadingAdapter keyEl
                }
                builder[keyEl.instance] = valueEl.instance
            }
            return@LoadingAdapter LoadResult.Instance(builder)
        },
        LoadingAdapter { path, hierarchy, type ->
            val ng = type.nonGeneric ?: return@LoadingAdapter LoadResult.Invalid
            if (ng != Property::class.java) return@LoadingAdapter LoadResult.Invalid
            if (type !is ParameterizedType) return@LoadingAdapter LoadResult.Invalid
            loadElement(
                ResolutionPath.FieldChild(ng.getDeclaredField("value").also { it.isAccessible = true }, path),
                hierarchy + hierarchy.last(),
                type.actualTypeArguments[0]
            ).map { Property.of(it) }
        },
        LoadingAdapter { path, hierarchy, type ->
            val ng = type.nonGeneric ?: return@LoadingAdapter LoadResult.Invalid
            if (!ng.isEnum) return@LoadingAdapter LoadResult.Invalid
            ng as Class<out Enum<*>>
            val el = hierarchy.last()!!.asJsonPrimitive
            LoadResult.Instance(ng.enumConstants.find { if (el.isString) it.name == el.asString else it.ordinal == el.asInt }!!)
        },
        LoadingAdapter(::loadClass),
    )


    fun <T : Any> loadConfig(root: JsonElement, clazz: Class<T>): LoadResult<T> {
        val result = loadElement(ResolutionPath.Root, listOf(root), clazz)
        logLoadResult(ResolutionPath.Root, result)
        return result
    }

    inline fun <reified T : Any> directLoader(crossinline block: (element: JsonElement) -> LoadResult<T>): LoadingAdapter<T> {
        return LoadingAdapter { path, hierarchy, type ->
            if (type.nonGeneric != T::class.java) LoadResult.Invalid
            else try {
                block(hierarchy.last()!!)
            } catch (e: Throwable) {
                LoadResult.Failure(e, path)
            }
        }
    }


    fun <T : Any> loadElement(path: ResolutionPath, hierarchy: List<JsonElement?>, clazz: Class<T>): LoadResult<T> {
        return loadElement(path, hierarchy, clazz as Type) as LoadResult<T>
    }

    fun loadElement(path: ResolutionPath, hierarchy: List<JsonElement?>, type: Type): LoadResult<Any?> {
        var bestResult: LoadResult<Any?> = LoadResult.Invalid
        for (adapter in adapters) {
            val adapt = try {
                adapter.adapt(path, hierarchy, type)
            } catch (e: Exception) {
                LoadResult.Failure(e, path)
            }
            if (adapt is LoadResult.Instance<*>) {
                bestResult = adapt
                break
            }
            bestResult = bestResult.or(adapt)
        }
        val event = ConfigMigrationEvent(this, path, hierarchy, type, bestResult).also {
            try {
                MinecraftForge.EVENT_BUS.post(it)
            } catch (e: Throwable) {
                it.value = it.value.or(LoadResult.Failure(e, path))
            }
        }
        if (event.value is LoadResult.Invalid) {
            return LoadResult.Failure(
                RuntimeException("Could not resolve a loader for ${type.typeName} (${type.nonGeneric})"),
                path
            )
        }
        return event.value
    }

    private fun loadClass(path: ResolutionPath, hierarchy: List<JsonElement?>, type: Type): LoadResult<Any?> {
        val ng = type.nonGeneric ?: return LoadResult.Invalid
        if (ng.isAnonymousClass || ng.isEnum || ng.isInterface || ng.isPrimitive) return LoadResult.Invalid
        val instance = ng.getDeclaredConstructor().also { it.isAccessible = true }.newInstance()
        require(ng.isInstance(instance)) // this is all we can check at runtime, sadly
        val toBeFilled = ng.allAccessibleFields.filter { it.isAnnotationPresent(Expose::class.java) }
        for (childField in toBeFilled) {
            val childPath = ResolutionPath.FieldChild(childField, path)
            when (
                val value = loadElement(
                    childPath,
                    hierarchy + hierarchy.last()?.asJsonObject?.get(childField.name),
                    childField.genericType
                )
            ) {
                is LoadResult.Instance -> childField.set(instance, value.instance)
                LoadResult.UseDefault -> {}
                is LoadResult.Failure -> logLoadResult(childPath, value)
                LoadResult.Invalid -> logLoadResult(childPath, value)
            }
        }
        return LoadResult.Instance(instance)
    }

    fun logLoadResult(path: ResolutionPath, loadResult: LoadResult<*>) {
        when (loadResult) {
            is LoadResult.Failure -> {
                allFailures.add(loadResult)
                logger.error(
                    "${path}: Encountered failure propagated from ${loadResult.path}",
                    loadResult.exception
                )
            }

            is LoadResult.Instance -> {}
            LoadResult.Invalid -> logger.info("${path}: Encountered Invalid load result.")
            LoadResult.UseDefault -> logger.info("${path}: Encountered UseDefault load result.")
        }
    }

    fun hasAnyFailure(): Boolean {
        return allFailures.any()
    }
}