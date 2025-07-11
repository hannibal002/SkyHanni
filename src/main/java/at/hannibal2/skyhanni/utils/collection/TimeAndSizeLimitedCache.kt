package at.hannibal2.skyhanni.utils.collection

import com.google.common.cache.Cache
import com.google.common.cache.RemovalCause
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

@Suppress("unused")
class TimeAndSizeLimitedCache<K : Any, V : Any>(
    maxSize: Long,
    expireAfterWrite: Duration,
    removalListener: ((K?, V?, RemovalCause) -> Unit)? = null,
) : CacheMap<K, V>() {

    override val cache: Cache<K, V> = buildCache {
        maximumSize(maxSize)
        expireAfterWrite(expireAfterWrite.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        setRemovalListener(removalListener)
    }
}
