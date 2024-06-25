package at.hannibal2.skyhanni.utils

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

class RecalculatingValue<T>(private val expireTime: Duration, private val calculation: () -> T) : ReadOnlyProperty<Any?, T> {

    private var currentValue = calculation()
    private var lastAccessTime = SimpleTimeMark.farPast()

    @Deprecated("use by RecalculatingValue instead")
    fun getValue(): T {
        if (lastAccessTime.passedSince() > expireTime) {
            currentValue = calculation()
            lastAccessTime = SimpleTimeMark.now()
        }
        return currentValue
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (lastAccessTime.passedSince() > expireTime) {
            currentValue = calculation()
            lastAccessTime = SimpleTimeMark.now()
        }
        return currentValue
    }
}
