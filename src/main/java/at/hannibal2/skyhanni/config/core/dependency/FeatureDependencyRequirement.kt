package at.hannibal2.skyhanni.config.core.dependency

/**
 * Declares feature dependencies that must be enabled before the annotated option can be edited.
 *
 * Accepted values inside [value]:
 * - Third-party IDs or enum names (e.g. "BINGO_NET" or "bingo_brewers").
 * - Config field references written as "QualifiedClass#field"; when the class part is omitted the
 *   declaring class of the annotated option is assumed. The property has to be of the type boolean. Its recommended to use a property
 *   with custom getter and setter to modify it if its not a boolean anyway.
 *   Examples:
 *     - "#useBB" (field on the same class)
 *     - "BingoNetConfig#useBN" (simple name resolved relative to the current package)
 *     - "at.hannibal2.skyhanni.config.features.MyConfig#myToggle" (fully qualified).
 *
 * Each annotation forms one requirement group. When [requireAll] is `true`, every value listed must
 * be enabled. When `false`, enabling any value inside the group satisfies it. Multiple annotations
 * can be stacked to express complex logic such as `(A && B) || (C && D)`.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
@Repeatable
annotation class FeatureDependencyRequirement(
    vararg val value: String,
    val requireAll: Boolean = true,
    val message: String = "",
)
