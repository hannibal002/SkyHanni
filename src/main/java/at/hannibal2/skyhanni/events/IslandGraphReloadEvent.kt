package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onIslandGraphReload")
class IslandGraphReloadEvent(val graph: Graph) : SkyHanniEvent()
