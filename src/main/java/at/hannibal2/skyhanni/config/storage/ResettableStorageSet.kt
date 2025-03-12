package at.hannibal2.skyhanni.config.storage
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

open class ResettableStorageSet {
    open fun reset() {
        val default = this::class.createInstance()
        // For every mutable property, set its value on `this` to the value from `default`.
        this::class.memberProperties
            .filterIsInstance<KMutableProperty1<Any, Any?>>()
            .forEach { prop ->
                try {
                    val wasAccessible = prop.isAccessible
                    prop.isAccessible = true
                    prop.set(this, prop.get(default))
                    prop.isAccessible = wasAccessible
                } catch (e: Exception) {
                    e.printStackTrace()
                    ErrorManager.skyHanniError("Failed to reset property ${prop.name} in ${this::class.simpleName}")
                }
            }
    }

    override fun toString(): String = this::class.memberProperties
        .filterIsInstance<KMutableProperty1<Any, Any?>>()
        .joinToString("\n") { prop ->
            val wasAccessible = prop.isAccessible
            prop.isAccessible = true
            "${prop.name} = ${(prop as KProperty1<Any, Any?>).get(this)}"
                .also { prop.isAccessible = wasAccessible }
        }
}
