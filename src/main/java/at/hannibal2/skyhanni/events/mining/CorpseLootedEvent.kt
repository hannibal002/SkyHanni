package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.mining.mineshaft.CorpeType

class CorpseLootedEvent(val corpseType: CorpeType, val loot: List<Pair<String, Int>>) : LorenzEvent()
