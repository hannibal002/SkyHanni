package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.events.LorenzEvent

class FossilExcavationEvent(val loot: List<Pair<String, Int>>): LorenzEvent()
