package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.Cache
import com.google.common.cache.RemovalCause
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

class TimeLimitedCache<K : Any, V : Any>(
    expireAfterWrite: Duration,
    removalListener: ((K?, V?, RemovalCause) -> Unit)? = null,
) : CacheMap<K, V>() {

    override val cache: Cache<K, V> = buildCache {
        expireAfterWrite(expireAfterWrite.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        setRemovalListener(removalListener)
    }
}
