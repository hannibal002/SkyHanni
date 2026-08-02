package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils

@SkyHanniModule
object NavigateAllHelper {

    private var route: List<LorenzVec> = listOf()
    private var total = 0
    private var currentNodeType: GraphNodeTag? = null

    private val allowedMultiNavigationTags = setOf(
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
        GraphNodeTag.HIDEYHO_HIDING_LOCATION,
    )

    private val pathfindCoroutine = CoroutineSettings("navigate all pathfind")

    /**
     * Navigate to all nodes with the selected [GraphNodeTag]
     *
     * In future this should be changed to take in a predicate for nodes
     * Existing features should be switched to use a more abstract version of this
     * These features include: Fast Fairy Souls, Spider Relic Pathfind, Shulker Finder
     */
    private fun navigateAll(nodeType: GraphNodeTag) {
        if (nodeType !in getValidTagNames()) {
            ChatUtils.userError("Target type is invalid on this island!")
            return
        }

        val graph = IslandGraphs.currentIslandGraph ?: return
        val targetNodes = graph.getNodesWithTags(nodeType)
        currentNodeType = nodeType

        pathfindCoroutine.launch {
            route = emptyList()
            route = calculateRoute(targetNodes)
            total = route.size

            ChatUtils.chat("Navigating to $total ${nodeType.displayName}")

            recursiveNavigate()
        }
    }

    private fun recursiveNavigate() {
        if (route.isEmpty()) return

        val target = route.first()
        route = route.drop(1)

        IslandGraphs.pathFind(
            target,
            "${currentNodeType?.displayName} ${total - route.size}/$total",
            onFound = {
                if (route.isEmpty()) currentNodeType = null
                recursiveNavigate()
            },
            condition = { currentNodeType != null },
        )
    }

    private fun calculateRoute(targetNodes: List<GraphNode>): List<LorenzVec> = NavigationUtils.getRoute(targetNodes)

    private fun handleSkip() {
        if (route.isEmpty()) {
            if (currentNodeType != null) {
                currentNodeType = null
            } else {
                ChatUtils.userError("No current navigation to skip. §eUse /shnavigateall to start navigation")
            }
            return
        }

        // TODO In future it should recalculate the route taking into account that we dont need the skipped node anymore
        recursiveNavigate()
    }

    @HandleEvent
    private fun onIslandLeave() {
        route = emptyList()
        total = 0
        currentNodeType = null
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
                BrigadierUtils.dynamicSuggestionProvider { getValidTagNames().map { it.cleanName } },
            ) { nodeType ->
                navigateAll(nodeType)
            }
            literalCallback("skip") {
                handleSkip()
            }
        }
    }

    private fun getValidTagNames(): Set<GraphNodeTag> {
        val activeTags = IslandGraphs.currentIslandGraph?.getActiveNodeTags() ?: return emptySet()
        return activeTags.filter { it in allowedMultiNavigationTags }.toSet()
    }
}
