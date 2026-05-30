package at.hannibal2.skyhanni.utils

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
)
// This class is to replace @Deprecated, which gets flagged by detekt
annotation class Legacy(
    val message: String,
    val replaceWith: ReplaceWith = ReplaceWith(""),
)
