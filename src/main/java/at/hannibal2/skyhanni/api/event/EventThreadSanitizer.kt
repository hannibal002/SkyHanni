package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.test.command.ErrorManager
import java.lang.Thread as JavaThread

object EventThreadSanitizer {
    val THREADS = mapOf<String, ThreadType>(
        "Render thread" to RENDER,
        "Netty Epoll IO" to NETWORK,
        "DefaultDispatcher-worker" to DISPATCHER,
    )

    // DefaultDispatcher-worker-1 -> DefaultDispatcher-worker
    // Netty Epoll IO #1 -> Netty Epoll IO
    private val regex = Regex("""(\s*#|-)\d+$""")

    private fun getCurrentThreadType(): ThreadType? {
        val threadName = JavaThread.currentThread().name.replace(regex, "")
        return THREADS[threadName]
    }

    fun checkThread(name: String, allowedThreads: Set<ThreadType>?) {
        val threads = allowedThreads.takeUnless { it.isNullOrEmpty() } ?: return
        if (ANY in threads) return

        val currentThreadType = getCurrentThreadType() ?: return
        if (currentThreadType !in threads) {
            ErrorManager.logErrorStateWithData(
                "Event $name was posted on thread $currentThreadType, but allowed threads are $threads",
                "Event posted from unexpected thread",
            )
        }
    }
}
