package at.hannibal2.skyhanni.config.storage
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

open class ResettableStorageSet {
    @Suppress("UNCHECKED_CAST")
    private val mutableMemberProperties: List<KMutableProperty1<Any, Any?>> =
        this::class.memberProperties.filterIsInstance<KMutableProperty1<Any, Any?>>()

    open fun reset() = applyFromOther(this::class.createInstance())

    open fun applyFromOther(other: ResettableStorageSet) {
        if (this::class != other::class) return
        mutableMemberProperties.forEach { prop ->
            try {
                val wasAccessible = prop.isAccessible
                prop.isAccessible = true
                prop.set(this, prop.get(other))
                prop.isAccessible = wasAccessible
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorManager.skyHanniError(
                    "Failed to apply property ${prop.name} from ${other::class.simpleName} to ${this::class.simpleName}"
                )
            }
        }
    }

    override fun toString(): String = mutableMemberProperties.joinToString("\n") { prop ->
            val wasAccessible = prop.isAccessible
            prop.isAccessible = true
            "${prop.name} = ${(prop as KProperty1<Any, Any?>).get(this)}"
                .also { prop.isAccessible = wasAccessible }
        }
}
