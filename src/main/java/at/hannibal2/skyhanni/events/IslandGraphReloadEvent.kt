package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.model.Graph

class IslandGraphReloadEvent(val graph: Graph) : SkyHanniEvent()
