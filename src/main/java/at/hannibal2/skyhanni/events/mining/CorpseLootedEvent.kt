package at.hannibal2.hanni.events.mining

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.mining.glacitemineshaft.CorpseType

class CorpseLootedEvent(val corpseType: CorpseType, val loot: List<Pair<String, Int>>) : HanniEvent()
