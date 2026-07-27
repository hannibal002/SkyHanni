package at.hannibal2.skyhanni.events.effects

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Fired when the tracked duration of a [NonGodPotEffect] changes.
 *
 * This event is posted by [at.hannibal2.skyhanni.data.effect.EffectApi] when an effect is gained,
 * removed, or its duration is updated via chat message or tab list.
 *
 * @param effect The effect whose duration changed.
 * @param durationChangeType How the duration changed: [EffectDurationChangeType.ADD] adds the given duration
 *   to the current remaining time, [EffectDurationChangeType.SET] replaces it, and
 *   [EffectDurationChangeType.REMOVE] marks the effect as no longer active.
 * @param duration The duration value associated with the change.
 *   Always `null` when [durationChangeType] is [EffectDurationChangeType.REMOVE].
 */
@PrimaryFunction("onEffectUpdate")
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
        // Applies a PARTIAL_SET update by replacing only the specified time units
        // and preserving lower-order units from the existing duration.
        //
        // Examples:
        // - 2h      -> updates hours only
        // - 2h 10m  -> updates hours and minutes, preserves seconds
        // - 2h 10m 5s -> replaces the entire duration
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
