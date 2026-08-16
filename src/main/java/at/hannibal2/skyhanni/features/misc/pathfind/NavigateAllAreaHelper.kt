package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.features.misc.pathfind.IslandAreaBackend.getAreaTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.StringUtils

/**
 * Like [NavigateAllHelper] ("/shnavall"), but scoped to the player's current area.
 *
 * To find which area a target node belongs to, we walk the graph outward from it
 * until we hit the nearest [GraphNodeTag.AREA]/[GraphNodeTag.SMALL_AREA] node.
 */
@SkyHanniModule
object NavigateAllAreaHelper {

    // Caches owning area per node; cleared on graph reload.
    private val ownerAreaCache = mutableMapOf<GraphNode, String?>()

    @HandleEvent(IslandGraphReloadEvent::class)
    private fun onIslandGraphReload() {
        ownerAreaCache.clear()
    }

    /** Nearest area/small-area node name to [this] node by graph distance. */
    private fun GraphNode.ownerArea(): String? = ownerAreaCache.getOrPut(this) {
        GraphUtils.findDijkstraDistances(this) { it.getAreaTag(useConfig = true) != null }
            .lastVisitedNode
            .takeIf { it.getAreaTag(useConfig = true) != null }
            ?.name
    }

    private fun navigateAllAreaCommand(nodeType: GraphNodeTag) {
        if (nodeType !in NavigateAllHelper.getValidTagNames()) {
            ChatUtils.userError("${nodeType.displayName} §cis invalid for navigation on this island!")
            return
        }

        val graph = IslandGraphs.currentIslandGraph ?: return
        val allNodes = graph.getNodesWithTags(nodeType)

        // Tracked live by IslandAreaBackend.
        val currentArea = IslandAreaBackend.currentArea.takeIf { it.isNotEmpty() && it != AreaNode.NO_AREA }

        val targetNodes = if (currentArea != null) {
            allNodes.filter { it.ownerArea() == currentArea }
        } else {
            // No tracked area — fall back to all nodes like /shnavall.
            allNodes
        }

        if (targetNodes.isEmpty()) {
            ChatUtils.userError(
                if (currentArea != null) {
                    "No ${nodeType.displayName} found in your current area (§r$currentArea§c)!"
                } else {
                    "No ${nodeType.displayName} found on this island!"
                },
            )
            return
        }

        NavigateAllHelper.navigateAll(
            targetNodes,
            nodeType.displayName,
            nodeType.color.toColor(),
            onFinish = {
                val location = currentArea?.let { " in §r$it" }.orEmpty()
                ChatUtils.chat(
                    "Reached all ${StringUtils.pluralize(targetNodes.size, nodeType.displayName, withNumber = true)}$location§e.",
                )
            },
            continueNavigationCondition = NavigationCondition.None,
            condition = { true },
        )
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shnavigateallarea") {
            description = "Use the path finder to go to all locations of a type inside your current area"
            aliases = listOf("shnavallarea")

            argCallback(
                "nodeType",
                EnumArgumentType.filtered<GraphNodeTag>(
                    { it.cleanName.replace(" ", "_") },
                    isGreedy = true,
                ) { it in NavigateAllHelper.allowedMultiNavigationTags },
                BrigadierUtils.dynamicSuggestionProvider { NavigateAllHelper.getValidTagNames().map { it.cleanName } },
            ) { nodeType ->
                navigateAllAreaCommand(nodeType)
            }
            literalCallback("skip") {
                NavigateAllHelper.handleSkip()
            }
            literalCallback("stop") {
                NavigateAllHelper.handleStop(manual = true)
            }
            simpleCallback {
                ChatUtils.userError("Usage: /shnavigateallarea <location type>")
            }
        }
    }
}
