package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalCause
import java.util.concurrent.ConcurrentMap

abstract class CacheMap<K : Any, V : Any> : MutableMap<K, V> {

    protected abstract val cache: Cache<K, V>

    override val size: Int get() = cache.size().toInt()

    override fun isEmpty(): Boolean = cache.size() == 0L

    override fun containsKey(key: K): Boolean = cache.getIfPresent(key) != null

    override fun containsValue(value: V): Boolean = value in values

    override fun get(key: K): V? = cache.getIfPresent(key)

    operator fun set(key: K, value: V) = cache.put(key, value)

    override fun put(key: K, value: V): V? {
        val previous = get(key)
        set(key, value)
        return previous
    }

    override fun remove(key: K): V? {
        val value = get(key) ?: return null
        cache.invalidate(key)
        return value
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach { (key, value) -> put(key, value) }
    }

    override fun clear() = cache.invalidateAll()

    override val keys: MutableSet<K> get() = getMap().keys

    override val values: MutableCollection<V> get() = getMap().values

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = getMap().entries

    /**
     * Modifications to the returned map are not supported and may lead to unexpected behavior.
     * This method is intended for read-only operations such as iteration or retrieval of values.
     *
     * This returning map and any view into that map via [Map.keys], [Map.values] or [Map.entries],
     * may return [Collection.size] values larger than the elements actually present during iteration.
     * This can lead to problems with kotlin's [Iterable.toSet], [Iterable.toList] (etc.) small collection
     * optimizations. Those methods (and similar ones) have optimizations for single element collections.
     * Since the [Collection.size] is checked first those methods will then not make any additional
     * checks when accessing the elements of the collection. This can lead to rare [NoSuchElementException].
     * Therefore, the direct constructors of [HashSet], [ArrayList] and similar are to be preferred,
     * since they make no such optimizations.
     *
     * @return A read-only view of the cache's underlying map.
     */
    private fun getMap(): ConcurrentMap<K, V> = cache.asMap()

    // Neither Cache nor MutableMap have a default toString function.
    // This is a simplified version of the default AbstractMap toString function.
    override fun toString(): String {
        return entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            val keyStr = if (key === this) "(this Map)" else key.toString()
            val valueStr = if (value === this) "(this Map)" else value.toString()
            "$keyStr=$valueStr"
        }
    }

    companion object {
        fun <K : Any, V : Any> of(block: CacheBuilder<K, V>.() -> Unit): CacheMap<K, V> {
            return object : CacheMap<K, V>() {
                override val cache: Cache<K, V> = buildCache(block)
            }
        }

        inline fun <K : Any, V : Any> buildCache(block: CacheBuilder<K, V>.() -> Unit): Cache<K, V> {
            @Suppress("UNCHECKED_CAST")
            return (CacheBuilder.newBuilder() as CacheBuilder<K, V>).apply(block).build()
        }

        /** Sets the removal listener of the CacheBuilder to it, if not null */
        fun <K : Any, V : Any> CacheBuilder<K, V>.setRemovalListener(
            listener: ((K?, V?, RemovalCause) -> Unit)? = null,
        ): CacheBuilder<K, V> {
            if (listener == null) return this
            return removalListener { notification ->
                listener(notification.key, notification.value, notification.cause)
            }
        }
    }

}
