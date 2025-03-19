package at.hannibal2.skyhanni.config.storage
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties

open class ResettableStorageSet {
    @Suppress("UNCHECKED_CAST")
    private val mutableMemberProperties: List<KMutableProperty1<Any?, Any?>> =
        this::class.memberProperties
            .filterIsInstance<KMutableProperty1<Any, Any?>>()
            .map { it as KMutableProperty1<Any?, Any?> }

    open fun reset() = applyFromOther(this::class.createInstance())

    open fun applyFromOther(other: ResettableStorageSet) {
        if (this::class != other::class) return
        mutableMemberProperties.forEach { prop ->
            try {
                prop.set(this, prop.get(other))
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorManager.skyHanniError(
                    "Failed to apply property ${prop.name} from ${other::class.simpleName} to ${this::class.simpleName}"
                )
            }
        }
    }
}
