package at.hannibal2.skyhanni.utils

import kotlin.time.Duration

object DelayedRun {
    val map = mutableMapOf<() -> Any, SimpleTimeMark>()

    fun runDelayed(duration: Duration, run: () -> Unit) {
        map[run] = SimpleTimeMark.now() + duration
    }

    fun runNextTick(run: () -> Unit) {
        map[run] = SimpleTimeMark.now()
    }

    fun checkRuns() {
        map.entries.removeIf { (runnable, time) ->
            val inPast = time.isInPast()
            if (inPast) {
                runnable()
            }
            inPast
        }
    }
}
