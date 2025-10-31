package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.model.Graph

class IslandGraphReloadEvent(val graph: Graph) : HanniEvent()
