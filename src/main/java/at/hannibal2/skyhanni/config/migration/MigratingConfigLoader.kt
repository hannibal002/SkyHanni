package at.hannibal2.skyhanni.config.migration

import at.hannibal2.skyhanni.config.migration.MigratingConfigLoader.LoadingAdapter
import at.hannibal2.skyhanni.events.ConfigMigrationEvent
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.Expose
import io.github.moulberry.moulconfig.observer.Property
import net.minecraftforge.common.MinecraftForge
import java.lang.reflect.*

val <T> Class<T>.allFields: List<Field>
    get() = this.declaredFields.toList() + (this.superclass?.allFields ?: listOf())
val <T> Class<T>.allAccessibleFields: List<Field>
    get() = allFields.also { it.forEach { it.isAccessible = true } }

val Type.nonGeneric: Class<*>?
    get() = when (this) {
        is ParameterizedType -> this.rawType.nonGeneric
        is Class<*> -> this
        is WildcardType -> this.upperBounds[0].nonGeneric
        is TypeVariable<*> -> this.bounds[0].nonGeneric
        else -> null
    }

object MigratingConfigLoader {

    interface ResolutionPath {
        val parent: ResolutionPath?
        val label: String
        fun path(): String = parent?.path()?.let { "$it." } + label

        object Root : ResolutionPath {
            override val parent: ResolutionPath? get() = null
            override val label: String get() = "Root"
        }

        data class FieldChild(val field: Field, override val parent: ResolutionPath) : ResolutionPath {
            override val label: String = field.name
        }

        data class IndirectChild(override val label: String, override val parent: ResolutionPath) : ResolutionPath
    }

    sealed interface LoadResult<out T> {
        fun <V> map(mapper: (T?) -> V?): LoadResult<V> {
            if (this is Instance<T>) {
                return Instance(mapper(this.instance))
            }
            return this as LoadResult<V>
        }

        fun or(other: LoadResult<@UnsafeVariance T>): LoadResult<T>

        data class Instance<T>(val instance: T?) : LoadResult<T> {
            override fun or(other: LoadResult<T>): LoadResult<T> {
                return this
            }
        }

        data class Failure(val exception: Throwable, val field: Field?) : LoadResult<Nothing> {
            override fun or(other: LoadResult<Nothing>): LoadResult<Nothing> {
                if (other is Invalid) return this
                return other
            }
        }

        object UseDefault : LoadResult<Nothing> {
            override fun or(other: LoadResult<Nothing>): LoadResult<Nothing> {
                if (other is Instance) return other
                return this
            }
        }

        object Invalid : LoadResult<Nothing> {
            override fun or(other: LoadResult<Nothing>): LoadResult<Nothing> {
                return other
            }
        }
    }

    fun interface LoadingAdapter<T> {
        fun adapt(field: Field?, hierarchy: List<JsonElement?>, type: Type): LoadResult<T>
    }

    val adapters = listOf(
        LoadingAdapter { field, hierarchy, type ->
            val ng = type.nonGeneric ?: return@LoadingAdapter LoadResult.Invalid
            if (!ng.isPrimitive) return@LoadingAdapter LoadResult.Invalid
            loadElement(
                field, hierarchy, mapOf(
                    java.lang.Integer.TYPE to java.lang.Integer::class.java,
                    java.lang.Boolean.TYPE to java.lang.Boolean::class.java,
                    java.lang.Short.TYPE to java.lang.Short::class.java,
                    java.lang.Float.TYPE to java.lang.Float::class.java,
                    java.lang.Double.TYPE to java.lang.Double::class.java,
                    java.lang.Long.TYPE to java.lang.Long::class.java,
                    java.lang.Byte.TYPE to java.lang.Byte::class.java,
                    java.lang.Character.TYPE to java.lang.Character::class.java,
                )[ng]!!
            )
        },
        directLoader { LoadResult.Instance(it.asString) },
        directLoader { LoadResult.Instance(it.asInt) },
        directLoader { LoadResult.Instance(it.asFloat) },
        directLoader { LoadResult.Instance(it.asLong) },
        directLoader { LoadResult.Instance(it.asDouble) },
        directLoader { LoadResult.Instance(it.asNumber) },
        directLoader { LoadResult.Instance(it.asBigDecimal) },
        directLoader { LoadResult.Instance(it.asBigInteger) },
        directLoader { LoadResult.Instance(it.asBoolean) },
        directLoader { LoadResult.Instance(it.asShort) },
        directLoader { LoadResult.Instance(it.asJsonObject) },
        directLoader { LoadResult.Instance(it.asJsonArray) },
        directLoader { LoadResult.Instance(it.asJsonPrimitive) },
        directLoader { LoadResult.Instance(it as JsonNull) },
        directLoader { LoadResult.Instance(it.asByte) },
        directLoader { LoadResult.Instance(it.asCharacter) },
        LoadingAdapter { field, hierarchy, type ->
            val ng = type.nonGeneric ?: return@LoadingAdapter LoadResult.Invalid
            if (ng != List::class.java) return@LoadingAdapter LoadResult.Invalid
            type as ParameterizedType
            val builder = mutableListOf<Any?>()
            for (jsonElement in hierarchy.last()!!.asJsonArray) {
                val x = loadElement(null, hierarchy + jsonElement, type.actualTypeArguments[0])
                if (x is LoadResult.Instance) {
                    builder.add(x.instance)
                } else if (x is LoadResult.UseDefault) {
                    return@LoadingAdapter LoadResult.Failure(
                        RuntimeException("Cannot UseDefault for list element"),
                        field
                    )
                } else {
                    return@LoadingAdapter x
                }
            }
            return@LoadingAdapter LoadResult.Instance(builder)
        },
        LoadingAdapter { field, hierarchy, type ->
            val ng = type.nonGeneric ?: return@LoadingAdapter LoadResult.Invalid
            if (ng != Map::class.java) return@LoadingAdapter LoadResult.Invalid
            type as ParameterizedType
            val builder = mutableMapOf<Any?, Any?>()
            for ((key, value) in (hierarchy.last()!! as JsonObject).entrySet()) {
                val keyEl = loadElement(null, hierarchy + JsonPrimitive(key), type.actualTypeArguments[0])
                if (keyEl !is LoadResult.Instance<*>) {
                    if (keyEl is LoadResult.UseDefault) {
                        return@LoadingAdapter LoadResult.Failure(
                            RuntimeException("Cannot UseDefault for map key"),
                            field
                        )
                    }
                    return@LoadingAdapter keyEl
                }
                val valueEl = loadElement(null, hierarchy + value, type.actualTypeArguments[0])
                if (valueEl !is LoadResult.Instance<*>) {
                    if (valueEl is LoadResult.UseDefault) {
                        return@LoadingAdapter LoadResult.Failure(
                            RuntimeException("Cannot UseDefault for map key"),
                            field
                        )
                    }
                    return@LoadingAdapter keyEl
                }
                builder[keyEl.instance] = valueEl.instance
            }
            return@LoadingAdapter LoadResult.Instance(builder)
        },
        LoadingAdapter { field, hierarchy, type ->
            val ng = type.nonGeneric ?: return@LoadingAdapter LoadResult.Invalid
            if (ng != Property::class.java) return@LoadingAdapter LoadResult.Invalid
            if (type !is ParameterizedType) return@LoadingAdapter LoadResult.Invalid
            loadElement(
                ng.getDeclaredField("value").also { it.isAccessible = true },
                hierarchy + hierarchy.last(),
                type.actualTypeArguments[0]
            ).map { Property.of(it) }
        },
        LoadingAdapter { field, hierarchy, type ->
            val ng = type.nonGeneric ?: return@LoadingAdapter LoadResult.Invalid
            if (!ng.isEnum) return@LoadingAdapter LoadResult.Invalid
            ng as Class<out Enum<*>>
            val el = hierarchy.last()!!.asJsonPrimitive
            LoadResult.Instance(ng.enumConstants.find { if (el.isString) it.name == el.asString else it.ordinal == el.asInt }!!)
        },
        LoadingAdapter(::loadClass),
    )


    fun <T : Any> loadConfig(root: JsonElement, clazz: Class<T>): LoadResult<T> {
        return loadElement(null, listOf(root), clazz)
    }

    inline fun <reified T : Any> loader(crossinline block: (field: Field?, hierarchy: List<JsonElement?>) -> LoadResult<T>): LoadingAdapter<T> {
        return LoadingAdapter { field, hierarchy, type ->
            if (type.nonGeneric != T::class.java) LoadResult.Invalid
            else block(field, hierarchy)
        }
    }

    inline fun <reified T : Any> directLoader(crossinline block: (element: JsonElement) -> LoadResult<T>): LoadingAdapter<T> {
        return LoadingAdapter { field, hierarchy, type ->
            if (type.nonGeneric != T::class.java) LoadResult.Invalid
            else block(hierarchy.last()!!)
        }
    }


    fun <T : Any> loadElement(field: Field?, hierarchy: List<JsonElement?>, clazz: Class<T>): LoadResult<T> {
        return loadElement(field, hierarchy, clazz as Type) as LoadResult<T>
    }

    fun loadElement(field: Field?, hierarchy: List<JsonElement?>, type: Type): LoadResult<Any?> {
        var bestResult: LoadResult<Any?> = LoadResult.Invalid
        for (adapter in adapters) {
            val adapt = try {
                adapter.adapt(field, hierarchy, type)
            } catch (e: Exception) {
                LoadResult.Failure(e, field)
            }
            if (adapt is LoadResult.Instance<*>) {
                bestResult = adapt
                break
            }
            bestResult = bestResult.or(adapt)
        }
        val event = ConfigMigrationEvent(field, hierarchy, type, bestResult).also {
            try {
                MinecraftForge.EVENT_BUS.post(it)
            } catch (e: Throwable) {
                it.value = it.value.or(LoadResult.Failure(e, field))
            }
        }
        if (event.value is LoadResult.Invalid) {
            return LoadResult.Failure(
                RuntimeException("Could not resolve a loader for ${type.typeName} (${type.nonGeneric})"),
                field
            )
        }
        return event.value
    }

    private fun loadClass(field: Field?, hierarchy: List<JsonElement?>, type: Type): LoadResult<Any?> {
        val ng = type.nonGeneric ?: return LoadResult.Invalid
        if (ng.isAnonymousClass || ng.isEnum || ng.isInterface || ng.isPrimitive) return LoadResult.Invalid
        val instance = ng.getDeclaredConstructor().also { it.isAccessible = true }.newInstance()
        require(ng.isInstance(instance)) // this is all we can check at runtime, sadly
        val toBeFilled = ng.allAccessibleFields.filter { it.isAnnotationPresent(Expose::class.java) }
        for (childField in toBeFilled) {
            when (
                val value = loadElement(
                    childField,
                    hierarchy + hierarchy.last()?.asJsonObject?.get(childField.name),
                    childField.genericType
                )
            ) {
                is LoadResult.Instance -> childField.set(instance, value.instance)
                LoadResult.UseDefault -> {}
                is LoadResult.Failure -> return value
                LoadResult.Invalid -> return value
            }
        }
        return LoadResult.Instance(instance)
    }

}