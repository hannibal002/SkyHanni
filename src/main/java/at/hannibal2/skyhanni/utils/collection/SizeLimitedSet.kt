package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.RemovalCause

class SizeLimitedSet<T : Any>(
    maxSize: Int,
    useWeakKeys: Boolean = false,
    removalListener: ((T?, RemovalCause) -> Unit)? = null,
) : CacheSet<T>() {

    @Suppress("unused")
    constructor(maxSize: Int, removalListener: ((T?, RemovalCause) -> Unit)? = null) :
        this(maxSize, useWeakKeys = false, removalListener)

    override val cache = SizeLimitedCache<T, Unit>(
        maxSize,
        useWeakKeys = useWeakKeys,
        removalListener.toMapListener(),
    )
}
