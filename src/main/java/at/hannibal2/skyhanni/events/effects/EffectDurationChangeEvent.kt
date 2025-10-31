package at.hannibal2.hanni.events.effects

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.effect.NonGodPotEffect
import kotlin.time.Duration

class EffectDurationChangeEvent(
    val effect: NonGodPotEffect,
    val durationChangeType: EffectDurationChangeType,
    val duration: Duration? = null
) : HanniEvent()

enum class EffectDurationChangeType {
    ADD,
    REMOVE,
    SET
}
