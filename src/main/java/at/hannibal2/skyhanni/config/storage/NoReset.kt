package at.hannibal2.skyhanni.config.storage

/**
 * Used in cases where you want to exclude a field present in a [Resettable] to not be reset.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class NoReset
