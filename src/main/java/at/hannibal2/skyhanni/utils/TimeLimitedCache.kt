package at.hannibal2.skyhanni.utils

import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class TimeLimitedCache<K, V>(
    expireAfterWrite: Duration,
    private val removalListener: (K?, V?) -> Unit = { _, _ -> },
) {

    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(expireAfterWrite.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        .removalListener { removalListener(it.key, it.value) }
        .build<K, V>()

    fun put(key: K, value: V) = cache.put(key, value)

    fun getOrNull(key: K): V? = cache.getIfPresent(key)

    fun clear() = cache.invalidateAll()

    fun values(): Collection<V> = cache.asMap().values

    fun keys(): Set<K> = cache.asMap().keys

    fun containsKey(key: K): Boolean = cache.getIfPresent(key) != null
    operator fun set(key: K, value: V) {
        put(key, value)
    }

    operator fun get(key: K): V? = getOrNull(key)
}
