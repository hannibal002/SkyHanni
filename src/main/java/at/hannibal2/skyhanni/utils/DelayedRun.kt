package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.drainTo
import net.minecraft.client.Minecraft
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration

@SkyHanniModule
object DelayedRun {

    private val tasks = mutableListOf<Pair<() -> Any?, SimpleTimeMark>>()
    private val futureTasks = ConcurrentLinkedQueue<Pair<() -> Any?, SimpleTimeMark>>()

    /**
     * Runs [runnable] at the end of the next game tick after [duration] has passed,
     * always on the main thread.
     */
    fun runDelayed(duration: Duration, label: String?, runnable: Runnable): SimpleTimeMark {
        val time = SimpleTimeMark.now() + duration
        futureTasks.add((runnable::run).withErrorHandling("DelayedRun.runDelayed", label) to time)
        return time
    }

    fun runDelayed(duration: Duration, runnable: Runnable): SimpleTimeMark = runDelayed(duration, null, runnable)

    fun <T> runDelayedReturning(duration: Duration, label: String?, block: () -> T): Pair<SimpleTimeMark, () -> T> {
        val time = SimpleTimeMark.now() + duration
        futureTasks.add(block.withErrorHandling("DelayedRun.runDelayedReturning", label) to time)
        return time to block
    }

    fun <T> runDelayedReturning(duration: Duration, block: () -> T): Pair<SimpleTimeMark, () -> T> =
        runDelayedReturning(duration, null, block)

    // TODO maybe rename to runOnNextMinecraftTick
    /**
     * Schedules a task via Minecraft's internal scheduler, which runs it on the main thread
     * at the start of the next game tick. The exact point relative to SkyHanni's own event
     * handlers is not guaranteed.
     */
    fun runNextTick(label: String?, runnable: Runnable) =
        Minecraft.getInstance().schedule(runnable.withErrorHandling("DelayedRun.runNextTick", label))

    @JvmStatic
    fun runNextTick(runnable: Runnable) = runNextTick(null, runnable)

    // TODO maybe rename to runAfterCurrentTickEvents
    /**
     * Runs at the end of the next game tick, after all other event handlers have processed.
     * Unlike [runNextTick], this goes through SkyHanni's own tick handler at [HandleEvent.LOWEST]
     * priority, guaranteeing that all event handlers for the current tick have finished first.
     * Use this when the task reads state that other handlers (e.g. chat handlers) may still
     * modify during the current tick.
     */
    fun runNextTickEnd(label: String?, runnable: Runnable) =
        futureTasks.add((runnable::run).withErrorHandling("DelayedRun.runNextTickEnd", label) to SimpleTimeMark.farPast())

    @JvmStatic
    fun runNextTickEnd(runnable: Runnable) = runNextTickEnd(null, runnable)

    /**
     * Runs [runnable] now if we are on the main thread, otherwise schedules it for the start of the
     * next game tick, same as [runNextTick].
     */
    fun runOrNextTick(label: String?, runnable: Runnable) =
        Minecraft.getInstance().execute(runnable.withErrorHandling("DelayedRun.runOrNextTick", label))

    fun runOrNextTick(runnable: Runnable) = runOrNextTick(null, runnable)

    /**
     * Runs [block], reporting a crash to the [ErrorManager] instead of letting it escape.
     * [source] names the scheduling method the task came from, [label] the caller's own description of the task.
     */
    private fun runWithErrorHandling(source: String, label: String?, block: () -> Any?) {
        try {
            block()
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(
                e,
                "Delayed task crashed while executing: ${e.message}",
                "label" to (label ?: "<none>"),
                "source" to source,
            )
        }
    }

    /**
     * Wraps [this] so that a crash is reported to the [ErrorManager] instead of taking down the game.
     * Minecraft's own executor has no error handling, so anything scheduled on it needs this.
     */
    private fun Runnable.withErrorHandling(source: String, label: String?) = Runnable {
        runWithErrorHandling(source, label) { run() }
    }

    /**
     * Wraps [this] the same way the [Runnable] variant does, for tasks stored as a function type.
     */
    internal fun (() -> Any?).withErrorHandling(source: String, label: String?): () -> Unit = {
        runWithErrorHandling(source, label, this)
    }

    @HandleEvent(priority = HandleEvent.LOWEST)
    private fun onTick() {
        tasks.removeIf { (block, time) ->
            val inPast = time.isInPast()
            if (inPast) block()
            inPast
        }
        futureTasks.drainTo(tasks)
    }
}
