package at.hannibal2.skyhanni.config.core.dependency

import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.lang.reflect.Field
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/** Aggregates dependency metadata for config fields/classes. */
object FeatureDependencyResolver {
    sealed interface DependencySource {
        data class BooleanField(
            val owner: Class<*>,
            val fieldName: String,
            val getter: (Any) -> Boolean,
            val property: KMutableProperty1<Any, Boolean>?,
            /** Field the dependency should jump to / get its config name from (delegate targets). */
            val effectiveFieldName: String = fieldName,
            /** Custom way to enable the dependency (e.g. third-party main toggle). */
            val enabler: ((Any) -> Unit)? = null,
        ) : DependencySource
    }

    data class Dependency(
        val label: String,
        val description: String,
        val source: DependencySource,
    )

    data class RequirementGroup(
        val dependencies: List<Dependency>,
        val requireAll: Boolean,
        val message: String,
    )

    data class Requirements(val groups: List<RequirementGroup>) {
        val isEmpty: Boolean get() = groups.isEmpty()
    }

    fun resolve(field: Field): Requirements {
        val annotations = mutableListOf<FeatureDependencyRequirement>()
        collectAnnotations(field, annotations)
        val thirdParty = null
        if (annotations.isEmpty()) return Requirements(emptyList())

        val groups = annotations.mapNotNull { group ->
            val deps = group.value.mapNotNull { parseDependency(it, field.declaringClass) }
            if (deps.isEmpty()) null else RequirementGroup(deps, group.requireAll, group.message)
        }.toMutableList()
        // A @ThirdPartyDependency acts as a requirement on the third party's main toggle,
        // except when the field itself is that main toggle (no self-dependency).
        return Requirements(groups)
    }


    private fun parseDependency(raw: String, context: Class<*>): Dependency? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val ownerPart = trimmed.substringBefore('#', "")
        val fieldName = trimmed.substringAfter('#', trimmed)
        val owner = when {
            ownerPart.isBlank() -> context
            ownerPart.contains('.') -> runCatching { Class.forName(ownerPart) }.getOrNull()
            else -> findRelativeClass(context, ownerPart)
        } ?: return null

        // Getter: resolve Kotlin property or Java field on-demand when getter is first invoked.
        val getter = fun(receiver: Any): Boolean {
            try {
                val kotlinProp = owner.kotlin.memberProperties
                    .firstOrNull { it.name == fieldName }
                    ?.takeIf { it.returnType.classifier == Boolean::class }
                    ?.also { it.isAccessible = true }
                if (kotlinProp != null) {
                    @Suppress("UNCHECKED_CAST")
                    val typed = kotlinProp as KProperty1<Any, *>
                    return (typed.get(receiver) as? Boolean) ?: false
                }
                val javaField = owner.declaredFields.firstOrNull {
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    it.name == fieldName && (it.type == java.lang.Boolean.TYPE || it.type == java.lang.Boolean::class.java)
                }
                    ?.apply { isAccessible = true }
                if (javaField != null) {
                    return javaField.getBoolean(receiver)
                }
            } catch (_: Throwable) {
                // ignore and fall through
            }
            return false
        }
        // Resolve the mutable Kotlin property so it can be enabled later via reflection.
        // Private delegate get/set properties have no backing field and are accessed
        // through the Kotlin property with isAccessible = true.
        val mutableProp: KMutableProperty1<Any, Boolean>? = runCatching {
            owner.kotlin.memberProperties
                .firstOrNull { it.name == fieldName }
                ?.takeIf { it is KMutableProperty1<*, *> && it.returnType.classifier == Boolean::class }
                ?.also { it.isAccessible = true }
                ?.let {
                    @Suppress("UNCHECKED_CAST")
                    it as KMutableProperty1<Any, Boolean>
                }
        }.getOrNull()
        // A delegate property references the real config option via @DependencyDelegate;
        // use the referenced field for the label and for jumping, while the delegate stays the gate.
        val effectiveFieldName = owner.kotlin.memberProperties
            .firstOrNull { it.name == fieldName }
            ?.findAnnotation<FeatureDependencyDelegate>()
            ?.field
            ?: fieldName
        val label = configOptionName(owner, effectiveFieldName) ?: effectiveFieldName
        return Dependency(
            label,
            "Requires '$label' to be enabled",
            DependencySource.BooleanField(owner, fieldName, getter, mutableProp, effectiveFieldName),
        )
    }

    /** Returns the user-facing config name of the given field, falling back to the internal name. */
    private fun configOptionName(owner: Class<*>, fieldName: String): String? {
        runCatching { owner.getDeclaredField(fieldName) }.getOrNull()?.let { javaField ->
            javaField.getAnnotation(ConfigOption::class.java)?.let { ann ->
                if (ann.name.isNotBlank()) return ann.name
            }
        }
        owner.kotlin.memberProperties.firstOrNull { it.name == fieldName }?.let { kp ->
            kp.findAnnotation<ConfigOption>()?.let { ann ->
                if (ann.name.isNotBlank()) return ann.name
            }
        }
        return null
    }

    private fun findRelativeClass(context: Class<*>, simpleName: String): Class<*>? {
        val packageName = context.`package`?.name ?: return null
        return runCatching { Class.forName("$packageName.$simpleName") }.getOrNull()
    }

    private fun collectAnnotations(field: Field, sink: MutableList<FeatureDependencyRequirement>) {
        field.getAnnotationsByType(FeatureDependencyRequirement::class.java).let { sink.addAll(it) }
        var owner: Class<*>? = field.declaringClass
        while (owner != null) {
            owner.getAnnotationsByType(FeatureDependencyRequirement::class.java).let { sink.addAll(it) }
            owner = owner.enclosingClass
        }
    }
}
