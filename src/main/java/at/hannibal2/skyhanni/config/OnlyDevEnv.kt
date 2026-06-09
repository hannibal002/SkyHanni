package at.hannibal2.skyhanni.config

/**
 * Hides the annotated config option outside the development environment. This
 * should be used for options that cannot work in production, as well as options
 * that are useful for debugging but may also be abused to cheat.
 *
 * Callers are still responsible for checking if the user is in a development
 * environment; the annotation alone does not prevent someone from manually
 * setting the option using `/shconfig` or in `config.json`.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class OnlyDevEnv
