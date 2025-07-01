package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import java.lang.reflect.Modifier
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Defines a storage set that can be reset to its default values.
 *  - vars will be set to their default value
 *  - mutable maps/collections will be cleared
 *  Params can be "ignored" from the reset by annotating them with [Transient].
 */
abstract class ResettableStorageSet {
    private val classSimpleName by lazy { this::class.simpleName ?: "UnknownClass" }
    private val defaults by lazy { this::class.createInstance() }
    private val props = this::class.memberProperties.filter { prop ->
        val annotatedIgnore = prop.hasAnnotation<Transient>()
        val modifiedIgnore = prop.javaField?.let { Modifier.isTransient(it.modifiers) } ?: false
        !annotatedIgnore && !modifiedIgnore
    }

    open fun reset() = props.forEach(::tryResetProp)

    private fun tryResetProp(prop: KProperty1<out ResettableStorageSet, *>) {
        val originalAccessibility = prop.isAccessible
        try {
            prop.isAccessible = true
            val current = prop.getter.call(this)
            prop.resetFun(current)
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

    private fun KProperty1<out ResettableStorageSet, *>.resetFun(current: Any?) = when {
        this is KMutableProperty1<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val mutableProp = this as KMutableProperty1<Any, Any?>
            val defaultValue = mutableProp.get(defaults)
            mutableProp.set(this@ResettableStorageSet, defaultValue)
        }
        current is MutableCollection<*> -> current.clear()
        current is MutableMap<*, *> -> current.clear()
        else -> ChatUtils.debug(
            "ResettableStorageSet $classSimpleName tried to reset property '${this.name}' " +
                "but it is of type ${current?.javaClass?.simpleName}, which is not handled."
        )
    }
}
