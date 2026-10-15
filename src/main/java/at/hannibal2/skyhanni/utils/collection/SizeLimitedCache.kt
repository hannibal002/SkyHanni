package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.Cache
import com.google.common.cache.RemovalCause

class SizeLimitedCache<K : Any, V : Any>(
    maxSize: Int,
    useWeakKeys: Boolean = false,
    removalListener: ((K?, V?, RemovalCause) -> Unit)? = null,
) : CacheMap<K, V>() {

    @Suppress("unused")
    constructor(maxSize: Int, removalListener: ((K?, V?, RemovalCause) -> Unit)? = null) :
        this(maxSize, useWeakKeys = false, removalListener)

    override val cache: Cache<K, V> = buildCache {
        if (useWeakKeys) weakKeys()
        maximumSize(maxSize.toLong())
        setRemovalListener(removalListener)
    }
}
