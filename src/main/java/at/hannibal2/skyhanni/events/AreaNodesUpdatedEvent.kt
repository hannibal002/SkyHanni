package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread

/**
 * Fired when area nodes are recalculated after player movement, graph reload, or config changes.
 * Nodes are sorted by distance, closest first.
 */
@Thread(DISPATCHER)
class AreaNodesUpdatedEvent : SkyHanniEvent()
