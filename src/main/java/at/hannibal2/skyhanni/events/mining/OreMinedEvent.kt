package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.mining.OreBlock

class OreMinedEvent(val originalOre: OreBlock, val map: Map<OreBlock, Int>) : LorenzEvent()
