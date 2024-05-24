package at.hannibal2.skyhanni.utils

import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class TimeLimitedCache<K: Any, V: Any>(
    expireAfterWrite: Duration,
    private val removalListener: (K?, V?) -> Unit = { _, _ -> },
): Iterable<Map.Entry<K, V>> {

    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(expireAfterWrite.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        .removalListener { removalListener(it.key, it.value) }
        .build<K, V>()

    fun put(key: K, value: V) = cache.put(key, value)

    fun getOrNull(key: K): V? = cache.getIfPresent(key)

    fun getOrPut(key: K, defaultValue: () -> V) = getOrNull(key) ?: defaultValue().also { put(key, it) }

    fun clear() = cache.invalidateAll()

    fun entries(): Set<Map.Entry<K, V>> = cache.asMap().entries

    fun values(): Collection<V> = cache.asMap().values

    fun keys(): Set<K> = cache.asMap().keys

    fun containsKey(key: K): Boolean = cache.getIfPresent(key) != null

    override fun iterator(): Iterator<Map.Entry<K, V>> = entries().iterator()
}
