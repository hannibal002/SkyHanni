package at.hannibal2.skyhanni.events

class SkillOverflowLevelupEvent(val skillName: String, val oldLevel: Int, val newLevel: Int): LorenzEvent()
