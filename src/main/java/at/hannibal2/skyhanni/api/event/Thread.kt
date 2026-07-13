package at.hannibal2.skyhanni.api.event

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Thread(vararg val values: ThreadType)

enum class ThreadType {
    ANY,
    RENDER,
    NETWORK,
    DISPATCHER,
}
