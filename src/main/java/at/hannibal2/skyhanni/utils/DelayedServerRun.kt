package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun.withErrorHandling
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.drainTo
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration

/**
 * Helper class for delaying execution until a specific server time mark.
 *
 * Unlike [DelayedRun], which uses client ticks, this class schedules tasks based on
 * server ticks. This makes it useful for timers and actions that should stay in sync
 * with the server tick rate and be affected by server lag.
 *
 * Tasks are queued from any context and are executed when the [ServerTickEvent] is
 * fired, which occurs on the networking thread.
 */
@SkyHanniModule
object DelayedServerRun {
    private val tasks = mutableListOf<Pair<() -> Any, ServerTimeMark>>()
    private val futureTasks = ConcurrentLinkedQueue<Pair<() -> Any, ServerTimeMark>>()

    fun runDelayed(duration: Duration, run: () -> Unit): ServerTimeMark {
        val time = ServerTimeMark.now() + duration
        futureTasks.add(run.withErrorHandling("DelayedServerRun.runDelayed") to time)
        return time
    }

    fun <T> runDelayedReturning(duration: Duration, run: () -> T): Pair<ServerTimeMark, () -> T> {
        val time = ServerTimeMark.now() + duration
        futureTasks.add(run.withErrorHandling("DelayedServerRun.runDelayedReturning") to time)
        return time to run
    }

    fun runNextTick(run: () -> Unit) =
        futureTasks.add(run.withErrorHandling("DelayedServerRun.runNextTick") to ServerTimeMark.farPast())

    @HandleEvent(priority = HandleEvent.LOWEST)
    private fun onServerTick() {
        tasks.removeIf { (runnable, time) ->
            val inPast = time.isInPast()
            if (inPast) runnable()
            inPast
        }
        futureTasks.drainTo(tasks)
    }
}
