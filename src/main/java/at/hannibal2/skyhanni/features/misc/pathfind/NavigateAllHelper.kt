package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils

@SkyHanniModule
object NavigateAllHelper {

    private val route: MutableList<LorenzVec> = mutableListOf()
    private var total = 0

    val allowedMultiNavigationTags = setOf(
        GraphNodeTag.HOPPITY,
        GraphNodeTag.RIFT_EFFIGY,
        GraphNodeTag.CRIMSON_MINIBOSS,
        GraphNodeTag.SPIDER_RELIC,
        GraphNodeTag.END_GOLEM,
        GraphNodeTag.FISHING_HOTSPOT,
        GraphNodeTag.FISHING_WORMHOLE,
        GraphNodeTag.FAIRY_SOUL,
        GraphNodeTag.HIDEONLEAF,
        GraphNodeTag.HIDEONSUN,
        GraphNodeTag.TREE_PROTECTION_ORDER,
        GraphNodeTag.HONEY_HIVE,
        GraphNodeTag.SAFARI_BELL,
    )

    /**
     * Navigate to all nodes with the selected tag
     *
     * In future this should be changed to take in a predicate for nodes
     * Existing features should be switched to use a more abstract version of this
     * These features include: Fast Fairy Souls, Spider Relic Pathfind, Shulker Finder
     *
     * As TSP algorithm is so quick, in future it should recalculate the remaining order
     * of nodes every few nodes reached for the most optimal pathing.
     *
     */
    private fun navigateAll(nodeTagType: GraphNodeTag) {
        if (nodeTagType !in getValidNodeNames()) {
            ChatUtils.userError("Target type is invalid on this island!")
            return
        }

        val graph = IslandGraphs.currentIslandGraph ?: return
        val list = graph.getNodesWithTags(nodeTagType)

        route.clear()
        route.addAll(NavigationUtils.getRoute(list, maxIterations = 300, neighborhoodSize = 50))
        total = route.size

        recursiveNavigate(nodeTagType)
    }

    private fun recursiveNavigate(nodeTagType: GraphNodeTag) {
        if (route.isEmpty()) return

        IslandGraphs.pathFind(
            route.removeFirstOrNull() ?: error("No more nodes found"),
            "${nodeTagType.displayName} ${total - route.size}/$total",
            onFound = { recursiveNavigate(nodeTagType) },
            condition = { SkyBlockUtils.inSkyBlock },
        )
    }

    @HandleEvent
    private fun onIslandChange() {
        route.clear()
        total = 0
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shnavigateall") {
            description = "Use the path finder to go to all locations of a type"
            aliases = listOf("shnavall")

            argCallback(
                "nodeType",
                EnumArgumentType.filtered<GraphNodeTag>(
                    { it.cleanName.replace(" ", "_") },
                    isGreedy = true,
                ) { it in allowedMultiNavigationTags },
                BrigadierUtils.dynamicSuggestionProvider { getValidNodeNames().map { it.cleanName } },
            ) { nodeType ->
                navigateAll(nodeType)
            }
        }
    }

    private fun getValidNodeNames(): List<GraphNodeTag> {
        val activeTags = IslandGraphs.currentIslandGraph?.getActiveNodeTags() ?: return emptyList()
        return activeTags.filter { it in allowedMultiNavigationTags }
    }

}
