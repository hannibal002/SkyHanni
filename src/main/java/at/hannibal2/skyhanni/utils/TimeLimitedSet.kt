package at.hannibal2.skyhanni.utils

import com.google.common.cache.RemovalCause
import kotlin.time.Duration

@Suppress("UnstableApiUsage")
class TimeLimitedSet<T : Any>(
    expireAfterWrite: Duration,
    private val removalListener: (T, RemovalCause) -> Unit = { _, _ -> },
) : MutableIterable<T> {

    private val cache = TimeLimitedCache<T, Unit>(expireAfterWrite) { key, _, cause ->
        key?.let {
            removalListener(it, cause)
        }
    }

    fun add(element: T) {
        cache[element] = Unit
    }

    operator fun plusAssign(element: T) = add(element)

    fun addIfAbsent(element: T) {
        if (!contains(element)) add(element)
    }

    fun remove(element: T) = cache.remove(element)

    operator fun minusAssign(element: T) {
        remove(element)
    }

    operator fun contains(element: T): Boolean = cache.containsKey(element)

    fun clear() = cache.clear()

    fun toSet(): MutableSet<T> = cache.keys

    override fun iterator(): MutableIterator<T> = toSet().iterator()
}
