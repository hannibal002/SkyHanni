package at.hannibal2.skyhanni.skyhannimodule

/**
 * This is used in parameterless @HandleEvent annotations.
 * e.g., IslandChangEvent -> onIslandChange to allow for
 * @HandleEvent
 * fun onIslandChange() { ... }
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrimaryFunction(val value: String)
