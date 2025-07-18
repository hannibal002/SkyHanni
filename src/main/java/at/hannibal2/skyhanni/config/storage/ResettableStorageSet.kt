package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import io.github.notenoughupdates.moulconfig.observer.Property
import java.lang.reflect.Modifier
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

/**
 * Defines a storage set that can be reset to its default values.
 *  - vars will be set to their default value
 *  - mutable maps/collections will be cleared
 *  Params can be "ignored" from the reset by annotating them with [Transient].
 */
abstract class ResettableStorageSet {
    private val classSimpleName by lazy { this::class.simpleName ?: "UnknownClass" }
    private val props = run {
        val vars = this::class.memberProperties.filterIsInstance<KMutableProperty1<out ResettableStorageSet, Any?>>()
        val others = listOf(
            MutableCollection::class,
            MutableMap::class,
            MutableIterator::class,
            Property::class,
            ResettableStorageSet::class
        ).flatMap { type ->
            this::class.memberProperties.filter {
                it.returnType.jvmErasure.isSubclassOf(type)
            }
        }
        (vars + others).filter { prop ->
            val annotatedIgnore = prop.hasAnnotation<Transient>()
            val modifiedIgnore = prop.javaField?.let { Modifier.isTransient(it.modifiers) } ?: false
            !annotatedIgnore && !modifiedIgnore
        }
    }

    open fun reset() {
        val defaults = this::class.createInstance()
        props.forEach { prop ->
            tryResetProp(prop, defaults)
        }
    }

    private fun tryResetProp(
        prop: KProperty1<out ResettableStorageSet, *>,
        defaults: ResettableStorageSet,
    ) {
        val originalAccessibility = prop.isAccessible
        try {
            prop.isAccessible = true
            val current = prop.getter.call(this)
            prop.resetFun(current, defaults)
            prop.isAccessible = originalAccessibility
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Failed to reset property ${prop.name} of $classSimpleName",
                "throwable message" to e.message,
            )
        } finally {
            prop.isAccessible = originalAccessibility
        }
    }

    private fun KProperty1<out ResettableStorageSet, *>.resetFun(
        current: Any?,
        defaults: ResettableStorageSet,
    ) = when {
        this is KMutableProperty1<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val mutableProp = this as KMutableProperty1<Any, Any?>
            val defaultValue = mutableProp.get(defaults)
            mutableProp.set(this@ResettableStorageSet, defaultValue)
        }
        current is Property<*> -> {
            @Suppress("UNCHECKED_CAST")
            val propRef = this as KProperty1<ResettableStorageSet, Property<Any?>>
            val defaultProp = propRef.get(defaults)
            @Suppress("UNCHECKED_CAST")
            val propCurrent = current as Property<Any?>
            propCurrent.set(defaultProp.get())
        }
        current is ResettableStorageSet -> current.reset()
        current is MutableCollection<*> -> current.clear()
        current is MutableMap<*, *> -> current.clear()
        current is MutableIterator<*> -> current.removeIf { true }
        else -> ChatUtils.debug(
            message = "ResettableStorageSet $classSimpleName tried to reset property '${this.name}' " +
                "but it is of type ${current?.javaClass?.simpleName}, which is not handled.",
            replaceSameMessage = true,
        )
    }
}
