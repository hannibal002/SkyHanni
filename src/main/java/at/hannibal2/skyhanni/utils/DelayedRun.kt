package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.drainTo
import net.minecraft.client.Minecraft
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration

// TODO add names for runs
@SkyHanniModule
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

    // TODO maybe rename to runOnNextMinecraftTick
    /**
     * Schedules a task via Minecraft's internal scheduler, which runs it on the main thread
     * at the start of the next game tick. The exact point relative to SkyHanni's own event
     * handlers is not guaranteed.
     */
    @JvmStatic
    fun runNextTick(runnable: Runnable) = Minecraft.getInstance().schedule(runnable)

    // TODO maybe rename to runAfterCurrentTickEvents
    /**
     * Runs at the end of the next game tick, after all other event handlers have processed.
     * Unlike [runNextTick], this goes through SkyHanni's own tick handler at [HandleEvent.LOWEST]
     * priority, guaranteeing that all event handlers for the current tick have finished first.
     * Use this when the task reads state that other handlers (e.g. chat handlers) may still
     * modify during the current tick.
     */
    @JvmStatic
    fun runNextTickEnd(runnable: Runnable) = futureTasks.add(runnable::run to SimpleTimeMark.farPast())

    /**
     * Runs [runnable] now if we are on the main thread, otherwise schedules it for the start of the
     * next game tick, same as [runNextTick].
     */
    fun runOrNextTick(runnable: Runnable) = Minecraft.getInstance().execute(runnable)

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onTick() {
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
