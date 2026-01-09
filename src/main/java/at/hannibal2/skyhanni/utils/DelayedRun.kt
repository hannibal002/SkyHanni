package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.drainTo
import net.minecraft.client.Minecraft
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration

// TODO add names for runs
object DelayedRun {

    private val tasks = mutableListOf<Pair<() -> Any, SimpleTimeMark>>()
    private val futureTasks = ConcurrentLinkedQueue<Pair<() -> Any, SimpleTimeMark>>()

    fun runDelayed(duration: Duration, run: () -> Unit): SimpleTimeMark {
        val time = SimpleTimeMark.now() + duration
        futureTasks.add(run to time)
        return time
    }

    /**
     * Runs in the next game tick (up to 50ms delay), always on the main thread.
     */
    fun runNextTick(run: () -> Unit) = Minecraft.getInstance().schedule(run)

    /**
     * Runs now if we are on the main thread, otherwise queues it for the next tick.
     */
    fun runOrNextTick(run: () -> Unit) = Minecraft.getInstance().execute(run)

    fun checkRuns() {
        tasks.removeIf { (runnable, time) ->
            val inPast = time.isInPast()
            if (inPast) {
                try {
                    runnable()
                } catch (e: Exception) {
                    ErrorManager.logErrorWithData(e, "DelayedRun task crashed while executing")
                }
            }
            inPast
        }
        futureTasks.drainTo(tasks)
    }
}
