package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.Cache
import com.google.common.cache.RemovalCause
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

@Suppress("UnstableApiUsage")
class TimeLimitedCache<K : Any, V : Any>(
    expireAfterWrite: Duration,
    removalListener: ((K?, V?, RemovalCause) -> Unit)? = null,
    useWeakKeys: Boolean = false,
) : CacheMap<K, V>() {

    constructor(
        expireAfterWrite: Duration,
        removalListener: ((K?, V?, RemovalCause) -> Unit)? = null,
    ) : this(expireAfterWrite, removalListener, useWeakKeys = false)

    override val cache: Cache<K, V> = buildCache {
        if (useWeakKeys) weakKeys()
        expireAfterWrite(expireAfterWrite.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        setRemovalListener(removalListener)
    }
}
