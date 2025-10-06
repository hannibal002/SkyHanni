package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import io.github.notenoughupdates.moulconfig.observer.Property
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure

private typealias KRProp = KProperty1<out Resettable, *>

/**
 * Defines a class that can be reset to its default values.
 *  - vars will be set to their default value
 *  - mutable maps/collections will be cleared
 *  Params can be "ignored" from the reset by annotating them with [Transient] or [NoReset].
 */
interface Resettable {
    companion object {
        private val propCache: MutableMap<KClass<*>, List<KRProp>> = mutableMapOf()
        private val otherMutableTypes: List<KClass<*>> = listOf(
            MutableCollection::class,
            MutableMap::class,
            MutableIterator::class,
            Property::class,
            Resettable::class,
        )
        private val isOtherMutableCache: MutableMap<KClass<*>, Boolean> = mutableMapOf()
        private fun KRProp.isOtherMutable() = isOtherMutableCache.getOrPut(returnType.jvmErasure) {
            otherMutableTypes.any { sc -> returnType.jvmErasure.isSubclassOf(sc) }
        }
        private fun KRProp.isIgnored() = hasAnnotation<Transient>() ||
            hasAnnotation<NoReset>() || javaField?.let { f ->
                f.isAnnotationPresent(NoReset::class.java) || Modifier.isTransient(f.modifiers)
            } ?: false
    }

    private val classSimpleName get() = this::class.simpleName ?: this::class.qualifiedName ?: "UnknownClass"

    fun reset() = with(this::class) {
        val defaults = createInstance()
        propCache.getOrPut(this) {
            this.memberProperties.filter { prop ->
                if (prop.isIgnored()) return@filter false
                prop is KMutableProperty1<out Resettable, *> || prop.isOtherMutable()
            }
        }.forEach { tryResetProp(it, defaults) }
    }

    private fun tryResetProp(prop: KProperty1<out Resettable, *>, defaults: Resettable) {
        val originalAccessibility = prop.isAccessible
        runCatching {
            prop.isAccessible = true
            val current = prop.getter.call(this)
            prop.internalResetFun(current, defaults)
        }.getOrElse { e ->
            ErrorManager.logErrorWithData(
                e,
                "Failed to reset property ${prop.name} of $classSimpleName",
                "throwable message" to e.message,
            )
        }
        prop.isAccessible = originalAccessibility
    }

    @Suppress("UNCHECKED_CAST")
    private fun KRProp.internalResetFun(current: Any?, defaults: Resettable) = when {
        this is KMutableProperty1<*, *> -> {
            val mutableProp = this as KMutableProperty1<Any, Any?>
            mutableProp.set(this@Resettable, mutableProp.get(defaults))
        }
        current is Property<*> -> {
            val propRef = this as KProperty1<Resettable, Property<Any?>>
            val propCurrent = current as Property<Any?>
            propCurrent.set(propRef.get(defaults).get())
        }
        current is Resettable -> current.reset()
        current is MutableCollection<*> -> current.clear()
        current is MutableMap<*, *> -> current.clear()
        current is MutableIterator<*> -> current.removeIf { true }
        else -> ChatUtils.debug(
            message = "Resettable $classSimpleName tried to reset property '${this.name}' " +
                "but it is of type ${current?.javaClass?.simpleName}, which is not handled.",
            replaceSameMessage = true,
        )
    }
}
