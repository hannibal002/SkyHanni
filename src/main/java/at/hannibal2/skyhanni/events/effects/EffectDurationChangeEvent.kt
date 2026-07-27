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
        /**
         * Applies a PARTIAL_SET update by constraining the existing ticking estimate
         * to the range implied by the displayed precision.
         *
         * Hypixel truncates omitted units:
         * - 1h      -> [1h, 2h)
         * - 1h 20m  -> [1h20m, 1h21m)
         * - 20m     -> [20m, 21m)
         * - 20s     -> [20s, 21s)
         *
         * The existing estimate is preserved if it falls within the valid range;
         * otherwise it is clamped to the nearest valid value.
         */
        fun updateUsingPartialSet(existing: Duration, duration: Duration): Duration {
            if (existing == Duration.ZERO) {
                return duration
            }

            val hours = duration.inWholeHours
            val minutes = duration.inWholeMinutes % 60
            val seconds = duration.inWholeSeconds % 60

            val hasHours = hours > 0
            val hasMinutes = minutes > 0
            val hasSeconds = seconds > 0

            val lowerBound = duration

            val upperBound = when {
                hasSeconds -> duration + 1.seconds
                hasMinutes -> duration + 1.minutes
                hasHours -> duration + 1.hours
                else -> duration + 1.seconds
            }

            return when {
                existing < lowerBound -> lowerBound
                existing >= upperBound -> upperBound - 1.seconds
                else -> existing
            }
        }
    }
}
