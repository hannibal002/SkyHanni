package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import com.google.common.cache.RemovalCause
import kotlin.time.Duration

@Deprecated("Use at.hannibal2.skyhanni.utils.collection.TimeLimitedSet import instead")
fun <T : Any> TimeLimitedSet(
    expireAfterWrite: Duration,
    removalListener: ((T?, RemovalCause) -> Unit)? = null,
): TimeLimitedSet<T> = TimeLimitedSet(expireAfterWrite, removalListener)
