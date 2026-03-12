package at.hannibal2.skyhanni.utils

import kotlin.time.Duration

class ConnectionRetryHelper(private val retryDelays: List<Duration>) {

    private var retryCount = 0
    private var nextRetry: SimpleTimeMark = SimpleTimeMark.farPast()

    val currentRetry: Int get() = retryCount
    val maxRetries: Int get() = retryDelays.size

    val retriesLabel get() = "($currentRetry / $maxRetries)"

    /**
     * Schedules the next retry and returns its delay, or null if all retries
     * are exhausted.
     */
    fun onFailure(): Duration? {
        val delay = retryDelays.getOrNull(retryCount) ?: return null
        nextRetry = SimpleTimeMark.now() + delay
        retryCount++
        return delay
    }

    fun reset() {
        retryCount = 0
        nextRetry = SimpleTimeMark.farPast()
    }
}
