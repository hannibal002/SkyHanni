package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.Cache
import com.google.common.cache.RemovalCause

class SizeLimitedCache<K : Any, V : Any>(
    maxSize: Long,
    removalListener: ((K?, V?, RemovalCause) -> Unit)? = null,
) : CacheMap<K, V>() {

    @Suppress("unused")
    constructor(maxSize: Int, removalListener: ((K?, V?, RemovalCause) -> Unit)? = null) :
        this(maxSize.toLong(), removalListener)

    override val cache: Cache<K, V> = buildCache {
        maximumSize(maxSize)
        setRemovalListener(removalListener)
    }
}
