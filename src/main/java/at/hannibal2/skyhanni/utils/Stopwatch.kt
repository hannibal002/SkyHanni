package at.hannibal2.skyhanni.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Stopwatch(
    private var duration: Duration = 0.seconds,
    private var paused: Boolean = true
) {
    private var startTime = if (paused) SimpleTimeMark.farPast() else SimpleTimeMark.now()

    fun start(lapIfStarted: Boolean = false) {
        if (!paused) {
            if (lapIfStarted) lap()
            return
        }
        paused = false
        startTime = SimpleTimeMark.now()
    }

    fun pause(revertLap: Boolean = false): Duration {
        if (paused) return 0.seconds
        paused = true
        val passedSince = startTime.passedSince()
        if (startTime != SimpleTimeMark.farPast() && !revertLap) {
            duration += passedSince
        }
        startTime = SimpleTimeMark.farPast()
        return passedSince
    }

    // hard set
    fun set(setDuration: Duration) {
        duration = setDuration
        if (!paused) startTime = SimpleTimeMark.now()
    }

    fun add(addedDuration: Duration) {
        val newDuration = duration + addedDuration
        duration = if (newDuration < 0.seconds) {
            0.seconds
        } else {
            newDuration
        }
    }

    // intended to be used for afk detection, call this whenever the player is detected to not be afk
    fun lap() {
        if (paused) return
        duration += startTime.passedSince()
        startTime = SimpleTimeMark.now()
    }

    // detection to pause tracker for afk timeout, don't need this if already paused
    fun getLapTime(): Duration? {
        if (paused) return null
        return startTime.passedSince()
    }

    fun getDuration(): Duration {
        if (paused) return duration
        return duration + startTime.passedSince()
    }

    fun isPaused(): Boolean = paused

    fun reset(pause: Boolean = true) {
        duration = 0.seconds
        paused = pause
        startTime = if (pause) SimpleTimeMark.farPast() else SimpleTimeMark.now()
    }
}
