package at.hannibal2.skyhanni.config.storage
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Provides an open interface for easily resetting a storage or data set to a default state.
 * All properties that are mutable and do not have the `@Transient` annotation will be reset on call.
 */
open class ResettableStorageSet {
    private val mutableMemberProperties: List<KMutableProperty1<Any, Any?>> =
        this::class.memberProperties.filter {
            it.visibility == KVisibility.PUBLIC
        }.filterIsInstance<KMutableProperty1<Any, Any?>>()

    open fun reset() = applyFromOther(this::class.createInstance())

    open fun applyFromOther(other: ResettableStorageSet) {
        if (this::class != other::class) return
        mutableMemberProperties.filter { prop ->
            prop.javaField != null
        }.forEach { prop ->
            try {
                prop.forceSet(prop.get(other))
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorManager.skyHanniError(
                    "Failed to apply property ${prop.name} from ${other::class.simpleName} to ${this::class.simpleName}"
                )
            }
        }
    }

    private fun KMutableProperty1<Any, Any?>.forceSet(value: Any?) {
        // Null as an Any? starts causing issues with casting, so we don't even want to try to set it.
        if (value == null) return
        val wasAccessible = this.isAccessible
        this.isAccessible = true
        this.set(this@ResettableStorageSet, value)
        this.isAccessible = wasAccessible
    }

    private fun KMutableProperty1<Any, Any?>.forceGet(): Any? {
        val wasAccessible = this.isAccessible
        this.isAccessible = true
        val value = this.get(this@ResettableStorageSet)
        this.isAccessible = wasAccessible
        return value
    }

    override fun toString(): String = mutableMemberProperties.joinToString("\n") { prop ->
        "${prop.name} = ${prop.forceGet() ?: ""}"
    }
}
