package at.hannibal2.skyhanni.utils

import kotlin.time.Duration

class TimeLimitedSet<T>(expireAfterWrite: Duration) {

    private val cache = TimeLimitedCache<T, Unit>(expireAfterWrite)

    fun add(element: T) = cache.put(element, Unit)

    fun contains(element: T): Boolean = cache.containsKey(element)

    fun clear() = cache.clear()

    fun toSet(): Set<T> = cache.keys().toSet()
}
