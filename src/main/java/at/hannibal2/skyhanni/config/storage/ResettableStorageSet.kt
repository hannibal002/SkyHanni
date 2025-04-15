package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

open class ResettableStorageSet {
    private val mutableMemberProperties: List<KMutableProperty1<Any, Any?>> =
        this::class.memberProperties.filterIsInstance<KMutableProperty1<Any, Any?>>()
            .filter { !it.hasAnnotation<Transient>() }

    open fun reset() = applyFromOther(this::class.createInstance())

    open fun applyFromOther(other: ResettableStorageSet) {
        if (this::class != other::class) return
        mutableMemberProperties.forEach { prop ->
            try {
                val otherPropVal = prop.forceGet(other)
                prop.forceSet(this, otherPropVal)
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorManager.skyHanniError(
                    "Failed to apply property ${prop.name} from ${other::class.simpleName} to ${this::class.simpleName}"
                )
            }
        }
    }

    private fun KMutableProperty1<Any, Any?>.forceGet(target: Any): Any? {
        val wasAccessible = this.isAccessible
        this.isAccessible = true
        val value = this.get(target)
        this.isAccessible = wasAccessible
        return value
    }

    private fun KMutableProperty1<Any, Any?>.forceSet(target: Any, value: Any?) {
        val wasAccessible = this.isAccessible
        this.isAccessible = true
        this.set(target, value)
        this.isAccessible = wasAccessible
    }

    override fun toString(): String = mutableMemberProperties.joinToString("\n") { prop ->
        "${prop.name} = ${prop.forceGet(this) ?: ""}"
    }
}
