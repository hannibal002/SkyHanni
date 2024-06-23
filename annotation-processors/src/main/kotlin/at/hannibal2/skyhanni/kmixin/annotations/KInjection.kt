package at.hannibal2.skyhanni.kmixin.annotations

// Inject
enum class InjectionKind {
    HEAD,
    TAIL,
    RETURN,
}

enum class TargetShift {
    NONE,
    BEFORE,
    AFTER,
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class KInject(
    val method: String,
    val kind: InjectionKind,
    val cancellable: Boolean = false,
    val captureLocals: Boolean = false,
    val remap: Boolean = true,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class KInjectAt(
    val method: String,
    val target: String,
    val shift: TargetShift = TargetShift.NONE,
    val ordinal: Int = -1,
    val cancellable: Boolean = false,
    val captureLocals: Boolean = false,
    val remap: Boolean = true,
)

// Redirect

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class KRedirectCall(
    val method: String,
    val target: String,
    val ordinal: Int = -1,
    val remap: Boolean = true,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class KRedirectField(
    val method: String,
    val target: String,
    val ordinal: Int = -1,
    val remap: Boolean = true,
)
