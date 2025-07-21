package at.hannibal2.skyhanni.events.item

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.NeuInternalName

class ShardGainEvent(val shardInternalName: NeuInternalName, val amount: Int) : SkyHanniEvent()
