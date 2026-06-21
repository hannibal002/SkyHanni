package at.hannibal2.skyhanni.utils

// This class is to replace @Deprecated, which gets flagged by detekt
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
