package at.hannibal2.skyhanni.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Stopwatch(
    private var duration: Duration = 0.seconds,
    private var paused: Boolean = true) {
    private var startTime = if (paused) SimpleTimeMark.farPast() else SimpleTimeMark.now()

    fun start() {
        if (!paused) return
        paused = false
        startTime = SimpleTimeMark.now()
    }

    fun pause() {
        if (paused) return
        paused = true
        if (startTime != SimpleTimeMark.farPast()) {
            duration += startTime.passedSince()
        }
        startTime = SimpleTimeMark.farPast()
    }

    fun getDuration(): Duration {
        if (paused) return duration
        return duration + startTime.passedSince()
    }

    fun isPaused(): Boolean = paused

    fun reset(pause: Boolean = true) {
        duration = 0.seconds
        paused = pause
        startTime = SimpleTimeMark.farPast()
    }
}
