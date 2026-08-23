package at.hannibal2.skyhanni.utils

/**
 * Alternative to [Deprecated] for things that can never be removed due to backwards compatibility reasons
 * (e.g. item categories that no longer exist but may appear on legacy items).
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
)
annotation class Legacy(
    val message: String,
    val replaceWith: ReplaceWith = ReplaceWith(""),
)
