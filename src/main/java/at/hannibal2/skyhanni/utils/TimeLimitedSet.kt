package at.hannibal2.skyhanni.utils

import com.google.common.cache.RemovalListener
import kotlin.time.Duration

class TimeLimitedSet<T>(
    expireAfterWrite: Duration,
    removalListener: RemovalListener<T, Unit>? = null
) {

    private val cache = TimeLimitedCache<T, Unit>(expireAfterWrite, removalListener)

    fun add(element: T) = cache.put(element, Unit)

    operator fun contains(element: T): Boolean = cache.containsKey(element)

    fun clear() = cache.clear()

    fun toSet(): Set<T> = cache.keys().toSet()
}
