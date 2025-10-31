package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.skillprogress.SkillType

// does not know how much exp is there, also gets called multiple times
class SkillExpGainEvent(val skill: SkillType, val gained: Double) : HanniEvent()
