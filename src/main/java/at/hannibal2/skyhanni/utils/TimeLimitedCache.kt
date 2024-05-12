package at.hannibal2.skyhanni.utils

import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class TimeLimitedCache<K, V>(expireAfterWrite: Duration) {

    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(expireAfterWrite.inWholeMilliseconds, TimeUnit.MILLISECONDS).build<K, V>()

    fun put(key: K, value: V) = cache.put(key, value)

    fun getOrNull(key: K): V? = cache.getIfPresent(key)

    fun getOrPut(key: K, defaultValue: () -> V): V {
        val value = cache.getIfPresent(key)
        return if (value == null) {
            val answer = defaultValue()
            put(key, answer)
            answer
        } else {
            value
        }
    }

    fun clear() = cache.invalidateAll()

    fun values(): Collection<V> = cache.asMap().values

    fun keys(): Set<K> = cache.asMap().keys

    fun containsKey(key: K): Boolean = cache.getIfPresent(key) != null
}
