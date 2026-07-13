package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/**
 * Fired when area nodes are recalculated after player movement, graph reload, or config changes.
 * Nodes are sorted by distance, closest first.
 */
class AreaNodesUpdatedEvent : SkyHanniEvent()
