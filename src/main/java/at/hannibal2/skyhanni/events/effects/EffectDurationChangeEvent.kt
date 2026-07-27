package at.hannibal2.skyhanni.events.effects

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class EffectDurationChangeEvent(
    val effect: NonGodPotEffect,
    val durationChangeType: EffectDurationChangeType,
    val duration: Duration? = null
) : SkyHanniEvent()

enum class EffectDurationChangeType {
    ADD,
    REMOVE,
    SET,
    PARTIAL_SET,
    ;

    companion object {
        fun updateUsingPartialSet(existing: Duration, duration: Duration): Duration {
            if (existing == Duration.ZERO) {
                return duration
            }
            val existingMinutes = existing.inWholeMinutes % 60
            val existingSeconds = existing.inWholeSeconds % 60
            val newHours = duration.inWholeHours
            val newMinutes = duration.inWholeMinutes % 60
            val hasSeconds = duration.inWholeSeconds % 60 != 0L
            val hasMinutes = newMinutes != 0L || hasSeconds
            val hasHours = newHours != 0L
            val result = when {
                // Full precision update (contains seconds)
                hasSeconds -> {
                    duration
                }
                // Minutes are known, seconds are not
                hasMinutes -> {
                    newHours.hours +
                        newMinutes.minutes +
                        existingSeconds.seconds
                }
                // Only hours are known
                hasHours -> {
                    newHours.hours +
                        existingMinutes.minutes +
                        existingSeconds.seconds
                }
                // Zero duration
                else -> {
                    duration
                }
            }
            return result
        }
    }
}
