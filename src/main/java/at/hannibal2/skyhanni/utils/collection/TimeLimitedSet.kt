package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.RemovalCause
import kotlin.time.Duration

class TimeLimitedSet<T : Any>(
    expireAfterWrite: Duration,
    removalListener: ((T?, RemovalCause) -> Unit)? = null,
) : CacheSet<T>() {

    override val cache = TimeLimitedCache<T, Unit>(
        expireAfterWrite,
        removalListener.toMapListener(),
    )
}
