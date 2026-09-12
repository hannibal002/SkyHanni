package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@SkyHanniModule
object SecondPassedEventManager {
    private val interval = 1.seconds

    /**
     * The shortest gap we are willing to leave between two posts. If catching up to the deadline
     * grid would leave less than this, the grid is re-anchored to now instead.
     */
    private val minimumGap = interval / 2

    private val timerSettings =
        CoroutineSettings("second passed timer", timeout = Duration.INFINITE).withIOContext()

    /** Set while a post is sitting in Minecraft's task queue, so a stalled main thread cannot cause a burst. */
    private val postQueued = AtomicBoolean(false)

    private var totalSeconds = 0

    @HandleEvent
    private fun onInitFinished() {
        timerSettings.launch {
            var deadline = TimeSource.Monotonic.markNow() + interval
            while (isActive) {
                delay(-deadline.elapsedNow())
                if (postQueued.compareAndSet(expectedValue = false, newValue = true)) {
                    DelayedRun.runOrNextTick(::postSecondPassed)
                }
                deadline += interval
                if (-deadline.elapsedNow() < minimumGap) {
                    deadline = TimeSource.Monotonic.markNow() + interval
                }
            }
        }
    }

    private fun postSecondPassed() {
        postQueued.store(false)
        if (!SkyBlockUtils.onHypixel) return
        SecondPassedEvent(totalSeconds).post()
        totalSeconds++
    }
}
