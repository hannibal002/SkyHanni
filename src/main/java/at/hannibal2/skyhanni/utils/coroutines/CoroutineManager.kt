package at.hannibal2.skyhanni.utils.coroutines

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface CoroutineManager {
    fun CoroutineSettings.launchCoroutine(block: suspend CoroutineScope.() -> Unit): Job
    fun CoroutineSettings.launchUnScopedCoroutine(block: suspend () -> Unit): Job

    fun <T> CoroutineSettings.asyncCoroutine(block: suspend CoroutineScope.() -> T): Deferred<T?>
    fun <T> CoroutineSettings.asyncUnScopedCoroutine(block: suspend () -> T): Deferred<T?>
}

// TODO when no more usages of these old functions remain, get rid of the compat class.
@Deprecated("Use CoroutineManager with CoroutineSettings options instead")
interface CompatCoroutineManager : CoroutineManager {
    @Deprecated(
        "Use launchCoroutine with CoroutineSettings options instead",
        ReplaceWith(
            "CoroutineSettings(name, timeout).launchCoroutine(block)",
            "at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings"
        )
    )
    fun launchCoroutine(name: String, timeout: Duration = 10.seconds, block: suspend CoroutineScope.() -> Unit): Job =
        CoroutineSettings(name, timeout).launchCoroutine(block)

    @Deprecated(
        "Use launchCoroutine with CoroutineSettings options instead",
        ReplaceWith(
            "CoroutineSettings(name, timeout).launchCoroutine(block)",
            "at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings"
        )
    )
    fun launchIOCoroutine(name: String, timeout: Duration = 10.seconds, block: suspend CoroutineScope.() -> Unit): Job =
        CoroutineSettings(name, timeout).withIOContext().launchCoroutine(block)

    @Deprecated(
        "Use launchUnScopedCoroutine with CoroutineSettings options instead",
        ReplaceWith(
            "CoroutineSettings(name, timeout).launchUnScopedCoroutine(block)",
            "at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings"
        )
    )
    fun launchNoScopeCoroutine(name: String, timeout: Duration = 10.seconds, block: suspend () -> Unit): Job =
        CoroutineSettings(name, timeout).launchUnScopedCoroutine(block)

    @Deprecated(
        "Use launchCoroutine with CoroutineSettings options instead",
        ReplaceWith(
            "CoroutineSettings(name, timeout).launchCoroutine(block)",
            "at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings"
        )
    )
    fun launchCoroutineWithMutex(
        name: String, mutex: Mutex, timeout: Duration = 10.seconds, block: suspend CoroutineScope.() -> Unit
    ): Job = CoroutineSettings(name, timeout).withMutex(mutex).launchCoroutine(block)

    @Deprecated(
        "Use launchCoroutine with CoroutineSettings options instead",
        ReplaceWith(
            "CoroutineSettings(name, timeout).launchCoroutine(block)",
            "at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings"
        )
    )
    fun launchIOCoroutineWithMutex(
        name: String, mutex: Mutex, timeout: Duration = 10.seconds, block: suspend CoroutineScope.() -> Unit
    ): Job = CoroutineSettings(name, timeout).withIOContext().withMutex(mutex).launchCoroutine(block)
}

@OptIn(InternalCoroutinesApi::class)
@Suppress("DEPRECATION")
class SkyHanniCoroutineManager(
    private val coroutineScope: CoroutineScope,
) : CompatCoroutineManager {

    /**
     * Launches a coroutine with the specified [block] and configuration options from the receiver [CoroutineSettings].
     * @receiver CoroutineSettings containing options for how to run the coroutine, such as timeout, IO context, and mutex.
     * @param block The suspend function to run within the coroutine, which will be wrapped with error handling, and the
     *  provided config options.
     */
    override fun CoroutineSettings.launchCoroutine(block: suspend CoroutineScope.() -> Unit): Job =
        coroutineScope.launch(CoroutineName("SkyHanni $name")) {
            runWithErrorHandling(block)
        }

    /**
     * Launches a coroutine with the specified [block] and configuration options from the receiver [CoroutineSettings],
     *  but without a CoroutineScope. This is useful for running code that doesn't need to be aware of the CoroutineScope, or needs its own.
     * @receiver CoroutineSettings containing options for how to run the coroutine, such as timeout, IO context, and mutex.
     * @param block The suspend function to run within the coroutine, which will be wrapped with error handling,
     *  and the provided config options.
     */
    override fun CoroutineSettings.launchUnScopedCoroutine(block: suspend () -> Unit): Job =
        launchCoroutine { block() }

    /**
     * Executes the given [block] asynchronously with the specified configuration options from the receiver [CoroutineSettings],
     *  and returns a Deferred result.
     * @receiver CoroutineSettings containing options for how to run the coroutine, such as timeout, IO context, and mutex.
     * @param block The suspend function to run within the coroutine, which will be wrapped with error handling,
     *  and the provided config options.
     * @return Deferred<T?> representing the result of the asynchronous computation, or null if an exception occurred.
     *  The Deferred will complete exceptionally if a [TimeoutCancellationException] occurs.
     */
    override fun <T> CoroutineSettings.asyncCoroutine(block: suspend CoroutineScope.() -> T): Deferred<T?> =
        coroutineScope.async(CoroutineName("SkyHanni $name")) {
            runWithErrorHandling(block)
        }

    /**
     * Executes the given [block] asynchronously with the specified configuration options from the receiver [CoroutineSettings],
     *  but without a CoroutineScope, and returns a Deferred result. This is useful for running code that doesn't need to be aware of
     *  the CoroutineScope, or needs its own.
     * @receiver CoroutineSettings containing options for how to run the coroutine, such as timeout, IO context, and mutex.
     * @param block The suspend function to run within the coroutine, which will be wrapped with error handling, and the provided config options.
     * @return Deferred<T?> representing the result of the asynchronous computation, or null if an exception occurred.
     *  The Deferred will complete exceptionally if a [TimeoutCancellationException] occurs.
     */
    override fun <T> CoroutineSettings.asyncUnScopedCoroutine(block: suspend () -> T): Deferred<T?> =
        asyncCoroutine { block() }

    /**
     * Wraps [block] with timeout, IO context, mutex, and error handling
     * as specified by the receiver [CoroutineSettings].
     */
    @Suppress("InjectDispatcher")
    private suspend fun <T> CoroutineSettings.runWithErrorHandling(
        block: suspend CoroutineScope.() -> T,
    ): T? {
        val wrappedBlock: suspend CoroutineScope.() -> T = when {
            this is MutexedCoroutineSettings && withIOContext -> ({ mutex.withLock { withContext(Dispatchers.IO, block) } })
            this is MutexedCoroutineSettings -> ({ mutex.withLock { block() } })
            withIOContext -> ({ withContext(Dispatchers.IO, block) })
            else -> block
        }

        return try {
            if (timeout != Duration.INFINITE && timeout > Duration.ZERO) withTimeout(timeout) { wrappedBlock() }
            // Note this is NOT "our" coroutineScope, but a function call from Kotlin.
            else coroutineScope { wrappedBlock() }
        } catch (e: TimeoutCancellationException) {
            if (PlatformUtils.isDevEnvironment) ErrorManager.logErrorWithData(
                e,
                "Coroutine $name timed out after $timeout",
                "coroutine name" to name,
                "timeout" to timeout,
            )
            throw e
        } catch (e: CancellationException) {
            val currentContext = currentCoroutineContext()
            val jobState = currentContext[Job]?.toString() ?: "unknown job"
            val cancellationCause = currentContext[Job]?.getCancellationException()
            SkyHanniMod.logger.debug("Job $jobState/$name was cancelled with cause: $cancellationCause", e)
            null
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(
                e,
                "Asynchronous exception caught in $name",
                "coroutine name" to name,
                "coroutine timeout" to timeout,
            )
            null
        }
    }
}

