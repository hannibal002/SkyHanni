package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.RemovalCause
import kotlin.time.Duration

class TimeLimitedSet<T : Any>(
    expireAfterWrite: Duration,
    weak: Boolean = false,
    removalListener: ((T?, RemovalCause) -> Unit)? = null,
) : CacheSet<T>() {

    constructor(
        expireAfterWrite: Duration,
        removalListener: ((T?, RemovalCause) -> Unit)? = null,
    ) : this(expireAfterWrite, weak = false, removalListener)

    override val cache = TimeLimitedCache<T, Unit>(
        expireAfterWrite,
        useWeakKeys = weak,
        removalListener.toMapListener(),
    )
}
