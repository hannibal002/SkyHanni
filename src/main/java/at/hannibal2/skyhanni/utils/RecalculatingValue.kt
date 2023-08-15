package at.hannibal2.skyhanni.utils

import kotlin.time.Duration

class RecalculatingValue<T>(private val expireTime: Duration, val calculation: () -> T) {
    private var currentValue = calculation()
    private var lastAccessTime = SimpleTimeMark.farPast()

    fun getValue(): T {
        if (lastAccessTime.passedSince() > expireTime) {
            currentValue = calculation()
            lastAccessTime = SimpleTimeMark.now()
        }
        return currentValue
    }
}