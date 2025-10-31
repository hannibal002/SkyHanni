package at.hannibal2.hanni.events.item

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.NeuInternalName

/**
 * All shard events, e.g. fusions and syphoning
 */
open class ShardEvent(val shardInternalName: NeuInternalName, val amount: Int) : HanniEvent()

/**
 * Shard events that are explicitly the player gaining shards. For use in stuff like profit trackers
 */
class ShardGainEvent(shardInternalName: NeuInternalName, amount: Int) : ShardEvent(shardInternalName, amount)

