package at.hannibal2.skyhanni.utils

import java.util.concurrent.LinkedBlockingQueue
import kotlin.time.Duration

object DelayedRun {
    val map = mutableListOf<Pair<() -> Any, SimpleTimeMark>>()
    private val inMap = LinkedBlockingQueue<Pair<() -> Any, SimpleTimeMark>>()

    fun runDelayed(duration: Duration, run: () -> Unit) {
        inMap.put(Pair(run, SimpleTimeMark.now() + duration))
    }

    fun runNextTick(run: () -> Unit) {
        inMap.put(Pair(run, SimpleTimeMark.farPast()))
    }

    fun checkRuns() {
        inMap.drainTo(map)
        map.removeIf { (runnable, time) ->
            time.isInPast().also { if (it) runnable() }
        }
    }
}
