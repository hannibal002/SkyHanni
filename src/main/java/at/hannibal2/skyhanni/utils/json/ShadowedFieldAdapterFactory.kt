package at.hannibal2.skyhanni.utils.json

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken

/**
 * Resolves duplicate @Expose field names that arise when a Kotlin override val property
 * overrides a parent property that is also annotated with @Expose. At the JVM level, both the
 * parent and the child have a backing field with the same name, and Gson normally throws
 * IllegalArgumentException on duplicate JSON field names.
 *
 * This factory detects such shadowed parent fields for the type being adapted and creates
 * a modified Gson instance that excludes them, so only the most-derived class's field is
 * used for serialization and deserialization.
 */
object ShadowedFieldAdapterFactory : TypeAdapterFactory {

    private val typesInProgress = ThreadLocal.withInitial<MutableSet<Class<*>>> { mutableSetOf() }

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        if (!typesInProgress.get().add(rawType)) return null

        return try {
            val shadowedFields = findShadowedParentFields(rawType)
            if (shadowedFields.isEmpty()) return null
            val strategy = ShadowedFieldExclusionStrategy(shadowedFields)
            BaseGsonBuilder.gson()
                .addSerializationExclusionStrategy(strategy)
                .addDeserializationExclusionStrategy(strategy)
                .create()
                .getAdapter(type)
        } finally {
            typesInProgress.get().remove(rawType)
        }
    }

    private fun findShadowedParentFields(rawType: Class<*>): Set<Pair<Class<*>, String>> = buildSet {
        val seenNames = mutableSetOf<String>()
        generateSequence(rawType) { clazz ->
            clazz.superclass?.takeIf { it != Any::class.java }
        }.flatMap { clazz ->
            clazz.declaredFields.filter {
                it.isAnnotationPresent(Expose::class.java)
            }.map { clazz to it.name }
        }.forEach { pair ->
            if (!seenNames.add(pair.second)) add(pair)
        }
    }

    private class ShadowedFieldExclusionStrategy(
        private val shadowedFields: Set<Pair<Class<*>, String>>,
    ) : ExclusionStrategy {
        override fun shouldSkipField(f: FieldAttributes): Boolean = (f.declaringClass to f.name) in shadowedFields
        override fun shouldSkipClass(clazz: Class<*>): Boolean = false
    }
}
