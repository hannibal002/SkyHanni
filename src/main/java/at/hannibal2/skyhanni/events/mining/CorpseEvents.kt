package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse.CorpseType
import at.hannibal2.skyhanni.utils.LorenzVec

class CorpseLocatedEvent(val corpseType: CorpseType, val location: LorenzVec) : SkyHanniEvent()
class CorpseLootedEvent(val corpseType: CorpseType, val loot: List<Pair<String, Int>>) : SkyHanniEvent()