package at.hannibal2.skyhanni.utils.coroutines

import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Represents the configuration for a coroutine run through [CoroutineManager].
 *
 * Options can be chained before launching:
 * ```
 * CoroutineConfig("myTask", timeout = 5.seconds)
 *     .withIOContext()
 *     .withMutex(myMutex)
 *     .launchCoroutine { ... }
 * ```
 *
 * @param name A descriptive name used in logging and coroutine naming.
 * @param timeout How long the coroutine may run before being canceled with a
 *   [kotlinx.coroutines.TimeoutCancellationException]. Use [Duration.INFINITE] to disable.
 * @param withIOContext Whether to run the block on [kotlinx.coroutines.Dispatchers.IO].
 */
open class CoroutineConfig(
    val name: String,
    val timeout: Duration = 10.seconds,
    val withIOContext: Boolean = false,
) {
    fun withMutex(mutex: Mutex): MutexedCoroutineConfig = MutexedCoroutineConfig(name, mutex, timeout, withIOContext)
    open fun withIOContext(): CoroutineConfig = CoroutineConfig(name, timeout, withIOContext = true)
}

/**
 * A [CoroutineConfig] that additionally holds a [Mutex], which will be acquired
 * before the coroutine block executes.
 */
class MutexedCoroutineConfig(
    name: String,
    val mutex: Mutex,
    timeout: Duration = 10.seconds,
    withIOContext: Boolean = false,
) : CoroutineConfig(name, timeout, withIOContext) {
    override fun withIOContext(): MutexedCoroutineConfig = MutexedCoroutineConfig(name, mutex, timeout, withIOContext = true)
}
