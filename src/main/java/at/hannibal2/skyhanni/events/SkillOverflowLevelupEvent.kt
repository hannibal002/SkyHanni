package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.misc.skillprogress.SkillType

class SkillOverflowLevelupEvent(val skill: SkillType, val oldLevel: Int, val newLevel: Int): LorenzEvent()
