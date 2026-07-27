package at.hannibal2.skyhanni.events.effects

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import kotlin.time.Duration

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
class EffectDurationChangeEvent(
    val effect: NonGodPotEffect,
    val durationChangeType: EffectDurationChangeType,
    val duration: Duration? = null
) : SkyHanniEvent()

enum class EffectDurationChangeType {
    ADD,
    REMOVE,
    SET
}
