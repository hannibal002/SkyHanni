package at.hannibal2.skyhanni.skyhannimodule

/**
 * Marks a Kotlin object as an event module, causing it to be added to the generated `LoadedModules` and registered
 * with `SkyHanniEvents`. Only functions declared directly inside such an object are registered as event handlers.
 *
 * This is retained at runtime, and therefore declared in the main source set rather than alongside the processor
 * that consumes it, so that module membership can be checked reflectively without relying on `LoadedModules`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SkyHanniModule(
    /**
     * If the module will only be loaded in a development environment.
     */
    val devOnly: Boolean = false,
)
