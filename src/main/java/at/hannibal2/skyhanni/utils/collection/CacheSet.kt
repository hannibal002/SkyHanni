package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.RemovalCause

abstract class CacheSet<T : Any> : MutableSet<T> {

    abstract val cache: CacheMap<T, Unit>

    override val size: Int get() = cache.size

    override fun isEmpty(): Boolean = cache.isEmpty()

    override operator fun contains(element: T): Boolean = cache.containsKey(element)

    override fun add(element: T): Boolean {
        return (element in cache).also { cache[element] = Unit }
    }

    override fun remove(element: T): Boolean {
        if (element !in cache) return false
        cache.remove(element)
        return true
    }

    fun addIfAbsent(element: T) {
        if (!contains(element)) add(element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        var value = false
        for (element in elements) {
            if (add(element)) value = true
        }
        return value
    }

    override fun clear() = cache.clear()

    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { it in cache }
    }

    override fun iterator(): MutableIterator<T> = keys.iterator()
    override fun retainAll(elements: Collection<T>): Boolean {
        var value = false
        for (key in keys) {
            if (key !in elements) {
                remove(key)
                value = true
            }
        }
        return value
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var value = false
        for (element in elements) {
            if (remove(element)) value = true
        }
        return value
    }

    private val keys: MutableSet<T>
        get() = cache.keys

    override fun toString(): String = keys.toString()

    companion object {

        fun <K : Any, V : Any> of(cacheMap: CacheMap<K, Unit>): CacheSet<K> {
            return object : CacheSet<K>() {
                override val cache: CacheMap<K, Unit> = cacheMap
            }
        }

        /** Changes a set removal listener to have the signature of a map's. Useful for creating CacheSets. */
        fun <K : Any, V : Any> ((K?, RemovalCause) -> Unit)?.toMapListener(): ((K?, V?, RemovalCause) -> Unit)? {
            if (this == null) return null
            return { key, _, cause -> invoke(key, cause) }
        }

    }

}
