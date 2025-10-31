package at.hannibal2.hanni.events.mining

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.mining.OreBlock

class OreMinedEvent(val originalOre: OreBlock?, val extraBlocks: Map<OreBlock, Int>) : HanniEvent()
