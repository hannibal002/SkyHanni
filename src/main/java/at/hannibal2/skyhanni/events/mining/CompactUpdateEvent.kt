package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.mining.OreBlock

class CompactUpdateEvent(val amount: Int, val block: OreBlock) : LorenzEvent()
