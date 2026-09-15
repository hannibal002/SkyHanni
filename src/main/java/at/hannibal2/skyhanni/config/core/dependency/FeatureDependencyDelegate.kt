package at.hannibal2.skyhanni.config.core.dependency

/**
 * Marks a delegate property that is used as a dependency gate for other config options.
 *
 * The [field] names the real config option (declared in the same class) that this
 * delegate represents. When the delegate is referenced as a dependency, the resolver
 * uses the referenced field for the displayed label and for jumping to the option
 * in the config, while the delegate itself stays the gate for the enabled check.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FeatureDependencyDelegate(val field: String)
