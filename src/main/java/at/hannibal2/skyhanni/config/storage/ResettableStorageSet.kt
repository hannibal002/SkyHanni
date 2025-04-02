package at.hannibal2.skyhanni.config.storage
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

open class ResettableStorageSet {
    private val mutableMemberProperties: List<KMutableProperty1<Any, Any?>> =
        this::class.memberProperties.filterIsInstance<KMutableProperty1<Any, Any?>>()

    open fun reset() = applyFromOther(this::class.createInstance())

    open fun applyFromOther(other: ResettableStorageSet) {
        if (this::class != other::class) return
        mutableMemberProperties.forEach { prop ->
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
