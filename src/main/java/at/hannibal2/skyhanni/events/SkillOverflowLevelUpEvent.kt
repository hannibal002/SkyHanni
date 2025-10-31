package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.skillprogress.SkillType

class SkillOverflowLevelUpEvent(val skill: SkillType, val oldLevel: Int, val newLevel: Int) : HanniEvent()
