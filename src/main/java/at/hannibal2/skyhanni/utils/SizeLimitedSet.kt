package at.hannibal2.skyhanni.utils

import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalCause
import com.google.common.cache.RemovalNotification

class SizeLimitedSet<T : Any>(
    maxSize: Long,
    private val removalListener: (T?, RemovalCause) -> Unit = { _, _ -> },
) : Iterable<T> {

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .removalListener { notification: RemovalNotification<T, Unit> ->
            removalListener(notification.key, notification.cause)
        }.build<T, Unit>()

    fun add(key: T) = cache.put(key, Unit)

    operator fun plusAssign(element: T) = add(element)

    fun remove(key: T) = cache.invalidate(key)

    operator fun minusAssign(element: T) = remove(element)

    operator fun contains(key: T): Boolean = cache.getIfPresent(key) != null

    fun clear() = cache.invalidateAll()

    fun toSet(): Set<T> = cache.asMap().keys.let { keys ->
        if (keys.isEmpty()) emptySet() else keys.toSet()
    }

    override fun iterator(): Iterator<T> = toSet().iterator()
}
