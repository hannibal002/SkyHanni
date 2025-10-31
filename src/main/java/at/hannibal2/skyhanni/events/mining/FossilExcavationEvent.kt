package at.hannibal2.hanni.events.mining

import at.hannibal2.hanni.api.event.HanniEvent

class FossilExcavationEvent(val loot: List<Pair<String, Int>>) : HanniEvent()
