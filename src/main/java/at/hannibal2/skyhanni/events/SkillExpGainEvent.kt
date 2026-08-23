package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType

class SkillExpGainEvent(
    val skill: SkillType,
    val gained: Double,
    val totalXp: Double?,
    val previousTotalXp: Double? = null,
    val source: String = "actionbar",
) : SkyHanniEvent()
