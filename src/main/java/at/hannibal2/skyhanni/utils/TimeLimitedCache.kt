package at.hannibal2.skyhanni.utils

import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalListener
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class TimeLimitedCache<K, V>(
    expireAfterWrite: Duration,
    removalListener: RemovalListener<K, V>? = null
) {

    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(expireAfterWrite.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        .apply { if (removalListener != null) removalListener(removalListener) }
        .build<K, V>()

    fun put(key: K, value: V) = cache.put(key, value)

    fun getOrNull(key: K): V? = cache.getIfPresent(key)

    fun clear() = cache.invalidateAll()

    fun values(): MutableCollection<V> = cache.asMap().values

    fun keys(): MutableSet<K> = cache.asMap().keys

    fun containsKey(key: K): Boolean = cache.getIfPresent(key) != null
}
