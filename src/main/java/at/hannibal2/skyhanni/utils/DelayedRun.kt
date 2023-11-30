package at.hannibal2.skyhanni.utils

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration

object DelayedRun {
    private val tasks = mutableListOf<Pair<() -> Any, SimpleTimeMark>>()
    private val futureTasks = ConcurrentLinkedQueue<Pair<() -> Any, SimpleTimeMark>>()

    fun runDelayed(duration: Duration, run: () -> Unit) {
        futureTasks.add(Pair(run, SimpleTimeMark.now() + duration))
    }

    fun runNextTick(run: () -> Unit) {
        futureTasks.add(Pair(run, SimpleTimeMark.farPast()))
    }

    fun checkRuns() {
        tasks.removeIf { (runnable, time) ->
            val inPast = time.isInPast()
            if (inPast) {
                runnable()
            }
            inPast
        }
        while (true)
            tasks.add(futureTasks.poll() ?: break)
    }
}
