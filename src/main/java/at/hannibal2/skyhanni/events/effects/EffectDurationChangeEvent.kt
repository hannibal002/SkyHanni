package at.hannibal2.skyhanni.events.effects

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import kotlin.time.Duration

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
