package at.hannibal2.hanni.hannimodule

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class HanniModule(
    /**
     * If the module will only be loaded in a development environment.
     */
    val devOnly: Boolean = false,
    /**
     * If the module will only be loaded while neu is installed
     */
    val neuRequired: Boolean = false,
)
