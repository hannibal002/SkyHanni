package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.RemovalCause
import kotlin.time.Duration

class TimeLimitedSet<T : Any>(
    expireAfterWrite: Duration,
    useWeakKeys: Boolean = false,
    removalListener: ((T?, RemovalCause) -> Unit)? = null,
) : CacheSet<T>() {

    constructor(
        expireAfterWrite: Duration,
        removalListener: ((T?, RemovalCause) -> Unit)? = null,
    ) : this(expireAfterWrite, useWeakKeys = false, removalListener)

    override val cache = TimeLimitedCache<T, Unit>(
        expireAfterWrite,
        useWeakKeys = useWeakKeys,
        removalListener.toMapListener(),
    )
}
