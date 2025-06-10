package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.mining.OreBlock
import at.hannibal2.skyhanni.utils.LorenzVec

class OreMinedEvent(
    val originalOre: OreBlock?,
    val extraBlocks: Map<OreBlock, Int>,
    val positions: Set<LorenzVec>,
) : SkyHanniEvent()
