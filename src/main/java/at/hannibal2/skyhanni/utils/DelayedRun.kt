package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import kotlin.time.Duration

// TODO find better sync bug fix than creating a new map for each use
object DelayedRun {
    var map = mapOf<() -> Any, SimpleTimeMark>()

    fun runDelayed(duration: Duration, run: () -> Unit) {
        map = map.editCopy {
            this[run] = SimpleTimeMark.now() + duration
        }
    }

    fun runNextTick(run: () -> Unit) {
        map = map.editCopy {
            this[run] = SimpleTimeMark.now()
        }
    }

    fun checkRuns() {
        if (map.isEmpty()) return
        map = map.editCopy {
            entries.removeIf { (runnable, time) ->
                val inPast = time.isInPast()
                if (inPast) {
                    runnable()
                }
                inPast
            }
        }
    }
}
