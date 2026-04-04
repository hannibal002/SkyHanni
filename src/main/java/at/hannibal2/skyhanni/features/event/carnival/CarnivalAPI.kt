package at.hannibal2.skyhanni.features.event.carnival

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.MayorChangeEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object CarnivalAPI {

    val isActive get() = Perk.CHIVALROUS_CARNIVAL.isActive
    val inCarnivalArea get() = isActive && inArea

    private var inArea = false
    private var isAreaHidden = false

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inArea = event.area == "Carnival"
    }

    @HandleEvent(MayorChangeEvent::class, onlyOnIsland = IslandType.HUB)
    fun onMayorChange() {
        if (isActive != isAreaHidden) return

        for (node in IslandGraphs.nodes("Carnival", GraphNodeTag.SMALL_AREA)) {
            node.enabled = isActive
        }
        val innerNode = IslandGraphs.node("Carnival Leader", GraphNodeTag.NPC)
        val list = IslandGraphs.nodesAround(innerNode) { it.enabled == !isActive }
        for (node in list) {
            node.enabled = isActive
        }

        isAreaHidden = !isActive
    }

}
