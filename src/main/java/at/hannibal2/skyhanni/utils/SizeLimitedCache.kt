package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.collection.SizeLimitedCache
import com.google.common.cache.RemovalCause

@Deprecated("Use at.hannibal2.skyhanni.utils.collection.SizeLimitedCache import instead")
fun <K : Any, V : Any> SizeLimitedCache(
    maxSize: Long,
    removalListener: ((K?, V?, RemovalCause) -> Unit)? = null,
): SizeLimitedCache<K, V> {
    return SizeLimitedCache(maxSize, removalListener)
}
