package at.hannibal2.skyhanni.config.storage
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties

open class ResettableStorageSet {
    open fun reset() {
        val default = this::class.createInstance()
        // For every *mutable* property (var), set its value on `this` to the value from `default`.
        this::class.memberProperties
            .filterIsInstance<KMutableProperty1<Any, Any?>>()
            .forEach { prop ->
                // Prop is declared as KMutableProperty1<Any, Any?>, but we know `this` is the same type at runtime.
                @Suppress("UNCHECKED_CAST")
                try {
                    (prop as KMutableProperty1<Any?, Any?>).set(this, prop.get(default))
                } catch (e: Exception) {
                    e.printStackTrace()
                    ErrorManager.skyHanniError("Failed to reset property ${prop.name} in ${this::class.simpleName}")
                }
            }
    }
}
