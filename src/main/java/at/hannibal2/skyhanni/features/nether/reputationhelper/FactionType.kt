package at.hannibal2.skyhanni.features.nether.reputationhelper

import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag

enum class FactionType(val factionName: String, val apiName: String) {
    BARBARIAN("Barbarian", "barbarians"),
    MAGE("Mage", "mages"),
    ;

    private val nodeSuffix = "($factionName)"

    fun getUndercoverAgentNode(): GraphNode = IslandGraphs.node("Undercover Agent $nodeSuffix", GraphNodeTag.NPC)

    fun getQuestBoardNode(): GraphNode = IslandGraphs.node("Quest Board $nodeSuffix", GraphNodeTag.POI)

    companion object {
        fun fromName(name: String): FactionType? = entries.firstOrNull { it.factionName.equals(name, ignoreCase = true) }
        fun fromAPIName(name: String): FactionType? = entries.firstOrNull { it.apiName.equals(name, ignoreCase = true) }
    }
}
