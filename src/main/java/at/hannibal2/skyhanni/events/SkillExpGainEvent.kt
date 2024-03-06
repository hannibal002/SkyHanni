package at.hannibal2.skyhanni.events

// does not know how much exp is there, also gets called multiple times
class SkillExpGainEvent(val skill: String) : LorenzEvent()
