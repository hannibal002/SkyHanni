package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext

@SkyHanniModule
object AsyncEventDispatchTracker {
    private val lock = Any()
    private val activeDispatches = mutableSetOf<Job>()
    private var acceptingDispatches = true

    suspend fun <T> dispatch(block: suspend () -> T): T = coroutineScope {
        val job = currentCoroutineContext()[Job] ?: error("Async event dispatch requires a coroutine job")
        synchronized(lock) {
            if (!acceptingDispatches) throw CancellationException("Async event dispatch rejected during client shutdown")
            activeDispatches.add(job)
        }
        try {
            block()
        } finally {
            synchronized(lock) {
                activeDispatches.remove(job)
            }
        }
    }

    @HandleEvent(priorityLevel = LOWEST)
    private fun onClientShutdown() {
        val dispatches = synchronized(lock) {
            acceptingDispatches = false
            activeDispatches.toList().also { activeDispatches.clear() }
        }
        dispatches.forEach { it.cancel(CancellationException("Client shutdown")) }
    }

    internal fun resetForTest() = synchronized(lock) {
        acceptingDispatches = true
        activeDispatches.clear()
    }
}
