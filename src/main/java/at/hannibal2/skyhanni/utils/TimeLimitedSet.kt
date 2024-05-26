package at.hannibal2.skyhanni.utils

import kotlin.time.Duration

class TimeLimitedSet<T: Any>(
    expireAfterWrite: Duration,
    private val removalListener: (T) -> Unit = {},
) {

    private val cache = TimeLimitedCache<T, Unit>(expireAfterWrite) { key, _ -> key?.let { removalListener(it) } }

    fun add(element: T) = cache.put(element, Unit)

    operator fun contains(element: T): Boolean = cache.containsKey(element)

    fun clear() = cache.clear()

    fun toSet(): Set<T> = cache.keys().toSet()
}
