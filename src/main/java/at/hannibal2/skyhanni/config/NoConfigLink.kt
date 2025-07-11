package at.hannibal2.skyhanni.config

// Some position elements don't need config links as they don't have a config option.
// Use this annotation to mark config elements that don't need a config link.
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class NoConfigLink
