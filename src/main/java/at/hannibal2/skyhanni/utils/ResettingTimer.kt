package at.hannibal2.skyhanni.utils

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)
class ResettingTimer {
    /* null represents the far past */
    var timeMark: TimeMark? = null
    fun reset() {
        timeMark = TimeSource.Monotonic.markNow()
    }

    fun hasPassed(duration: Duration): Boolean {
        return timeMark?.plus(duration)?.hasPassedNow() ?: true
    }

    fun resetIfHasPassed(duration: Duration): Boolean {
        val passed = hasPassed(duration)
        if (passed) {
            reset()
        }
        return passed
    }
}