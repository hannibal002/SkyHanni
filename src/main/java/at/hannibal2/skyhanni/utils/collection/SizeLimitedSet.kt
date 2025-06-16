package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.RemovalCause

class SizeLimitedSet<T : Any>(
    maxSize: Long,
    removalListener: ((T?, RemovalCause) -> Unit)? = null,
) : CacheSet<T>() {

    @Suppress("unused")
    constructor(maxSize: Int, removalListener: ((T?, RemovalCause) -> Unit)? = null) :
        this(maxSize.toLong(), removalListener)

    override val cache = SizeLimitedCache<T, Unit>(
        maxSize,
        removalListener.toMapListener(),
    )
}
