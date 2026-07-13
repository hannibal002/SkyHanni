package at.hannibal2.skyhanni.skyhannimodule

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Thread(vararg val value: ThreadType)

enum class ThreadType {
    ANY,
    RENDER,
    NETWORK,
    DISPATCHER,
}
