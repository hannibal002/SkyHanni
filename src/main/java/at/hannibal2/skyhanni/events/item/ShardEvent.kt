package at.hannibal2.skyhanni.events.item

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.NeuInternalName

/**
 * All shard events, e.g. fusions and syphoning
 */
open class ShardEvent(val shardInternalName: NeuInternalName, val amount: Int) : SkyHanniEvent()

/**
 * Shard events that are explicitly the player gaining shards. For use in stuff like profit trackers
 */
class ShardGainEvent(shardInternalName: NeuInternalName, amount: Int) : ShardEvent(shardInternalName, amount)

