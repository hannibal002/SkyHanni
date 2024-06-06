package at.hannibal2.skyhanni.skyhannimodule

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SkyHanniModule(
    /**
     * If the module will only be loaded in a development environment.
     */
    val inDevelopment: Boolean = false,
)
