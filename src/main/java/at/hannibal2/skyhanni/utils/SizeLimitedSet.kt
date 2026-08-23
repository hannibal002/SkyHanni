package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.collection.SizeLimitedSet
import com.google.common.cache.RemovalCause

@Deprecated("Use at.hannibal2.skyhanni.utils.collection.SizeLimitedSet import instead")
fun <T : Any> SizeLimitedSet(
    maxSize: Long,
    removalListener: ((T?, RemovalCause) -> Unit)? = null,
): SizeLimitedSet<T> {
    return SizeLimitedSet(maxSize, removalListener)
}
