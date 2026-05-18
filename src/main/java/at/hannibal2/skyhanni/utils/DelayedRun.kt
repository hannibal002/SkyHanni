package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.drainTo
import net.minecraft.client.Minecraft
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration

// TODO add names for runs
object DelayedRun {

    private val tasks = mutableListOf<Pair<() -> Any?, SimpleTimeMark>>()
    private val futureTasks = ConcurrentLinkedQueue<Pair<() -> Any?, SimpleTimeMark>>()

    /**
     * Runs [runnable] at the end of the next game tick after [duration] has passed,
     * always on the main thread.
     */
    fun runDelayed(duration: Duration, runnable: Runnable): SimpleTimeMark {
        val time = SimpleTimeMark.now() + duration
        futureTasks.add(runnable::run to time)
        return time
    }

    fun <T> runDelayedReturning(duration: Duration, block: () -> T): Pair<SimpleTimeMark, () -> T> {
        val time = SimpleTimeMark.now() + duration
        futureTasks.add(block to time)
        return time to block
    }

    /**
     * Runs [runnable] at the start of the next game tick, always on the main thread.
     */
    @JvmStatic
    fun runNextTick(runnable: Runnable) = Minecraft.getInstance().schedule(runnable)

    /**
     * Runs [runnable] at the end of the next game tick, always on the main thread.
     *
     * Prefer [runNextTick] unless you have a specific reason to use this method.
     */
    @JvmStatic
    fun runNextTickEnd(runnable: Runnable) = futureTasks.add(runnable::run to SimpleTimeMark.farPast())

    /**
     * Runs [runnable] now if we are on the main thread,
     * otherwise queues it for the start of the next game tick.
     */
    fun runOrNextTick(runnable: Runnable) = Minecraft.getInstance().execute(runnable)

    fun checkRuns() {
        tasks.removeIf { (block, time) ->
            val inPast = time.isInPast()
            if (inPast) {
                try {
                    block()
                } catch (e: Exception) {
                    ErrorManager.logErrorWithData(e, "DelayedRun task crashed while executing: ${e.message}")
                }
            }
            inPast
        }
        futureTasks.drainTo(tasks)
    }
}
