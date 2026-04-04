package at.hannibal2.skyhanni.utils.coroutines

import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Represents the declaration of, and execution for a coroutine run through [CoroutineManager].
 *
 * Options can be chained before launching:
 * ```
 * CoroutineSettings("myTask", timeout = 5.seconds)
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
open class CoroutineSettings(
    val name: String,
    val timeout: Duration = 10.seconds,
    internal var withIOContext: Boolean = false,
) {
    fun withMutex(mutex: Mutex?): CoroutineSettings = mutex?.let {
        MutexedCoroutineSettings(name, it, timeout, withIOContext)
    } ?: this
    fun withIOContext(): CoroutineSettings = this.apply { withIOContext = true }
}

/**
 * A [CoroutineSettings] that additionally holds a [Mutex], which will be acquired
 * before the coroutine block executes.
 */
open class MutexedCoroutineSettings(
    name: String,
    val mutex: Mutex,
    timeout: Duration = 10.seconds,
    withIOContext: Boolean = false,
) : CoroutineSettings(name, timeout, withIOContext)
