package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import com.google.common.cache.RemovalCause
import kotlin.time.Duration

@Deprecated("Use at.hannibal2.skyhanni.utils.collection.TimeLimitedCache import instead")
fun <K : Any, V : Any> TimeLimitedCache(
    expireAfterWrite: Duration,
    removalListener: ((K?, V?, RemovalCause) -> Unit)? = null,
): TimeLimitedCache<K, V> = TimeLimitedCache(expireAfterWrite, removalListener)
