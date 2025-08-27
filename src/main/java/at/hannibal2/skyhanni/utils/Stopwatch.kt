package at.hannibal2.skyhanni.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Stopwatch(startDuration: Duration = 0.seconds) {
    private var duration = startDuration
    private var paused: Boolean = true
    private var startTime = SimpleTimeMark.farPast()

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

    fun reset() {
        duration = 0.seconds
        paused = true
        startTime = SimpleTimeMark.farPast()
    }
}
