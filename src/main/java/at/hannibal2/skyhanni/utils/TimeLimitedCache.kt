package at.hannibal2.skyhanni.utils

import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class TimeLimitedCache<K, V>(expireAfterWrite: Duration) {

    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(expireAfterWrite.inWholeMilliseconds, TimeUnit.MILLISECONDS).build<K, V>()

    // TODO IntelliJ cant replace this, find another way?
//     @Deprecated("outdated", ReplaceWith("[key] = value"))
    @Deprecated("outdated", ReplaceWith("set(key, value)"))
    fun put(key: K, value: V) = set(key, value)

    fun getOrNull(key: K): V? = cache.getIfPresent(key)

    fun clear() = cache.invalidateAll()

    fun values(): MutableCollection<V> = cache.asMap().values

    fun keys(): MutableSet<K> = cache.asMap().keys

    fun containsKey(key: K): Boolean = cache.getIfPresent(key) != null

    operator fun set(key: K, value: V) {
        cache.put(key, value)
    }
}
