package at.hannibal2.skyhanni.utils.coroutines

import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Represents a configuration for a coroutine that will be run through the manager.
 */
open class CoroutineConfig(
    val name: String,
    val timeout: Duration = 10.seconds,
    val withIOContext: Boolean = false,
) {
    fun withMutex(mutex: Mutex): MutexedCoroutineConfig = MutexedCoroutineConfig(name, mutex, timeout)
    fun withIOContext(): CoroutineConfig = CoroutineConfig(name, timeout, withIOContext = true)
}

class MutexedCoroutineConfig(
    name: String,
    val mutex: Mutex,
    timeout: Duration = 10.seconds,
) : CoroutineConfig(name, timeout)
