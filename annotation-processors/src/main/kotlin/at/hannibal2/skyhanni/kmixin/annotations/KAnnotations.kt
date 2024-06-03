package at.hannibal2.skyhanni.kmixin.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class KMixin(
    val value: KClass<*>,
    val priority: Int = 1000,
    val remap: Boolean = true,
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class KPseudoMixin(
    val value: String,
    val priority: Int = 1000,
    val remap: Boolean = true,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class KStatic

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class KShadow(
    val kind: ShadowKind = ShadowKind.FIELD,
)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class KSelf

enum class ShadowKind {
    FIELD,
    METHOD,
    FINAL_FIELD,
}
