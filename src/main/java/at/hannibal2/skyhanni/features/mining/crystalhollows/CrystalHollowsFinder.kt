package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.features.misc.pathfind.AreaNode
import at.hannibal2.skyhanni.features.misc.pathfind.IslandAreaBackend.getAreaTag
import at.hannibal2.skyhanni.features.misc.pathfind.NavigateAllApi
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationCondition
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import net.minecraft.client.player.LocalPlayer

@SkyHanniModule
object CrystalHollowsFinder {


    @HandleEvent(onlyOnSkyblock = true)
    private fun onLocalPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        println("player move event")
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shfindch") {
            description = "Resets the Foraging Tracker."
            category = CommandCategory.USERS_RESET
            simpleCallback { doCommand() }
        }
    }

    private fun doCommand() {
        if (!IslandType.CRYSTAL_HOLLOWS.isInIsland()) {
            ChatUtils.userError("Only works in Crystal Hollows!")
            return
        }
        val graph = IslandGraphs.currentIslandGraph ?: run {
            ChatUtils.userError("currentIslandGraph is null")
            return
        }

        val closestNode = IslandGraphs.closestNode ?: run {
            ChatUtils.userError("closestNode is null")
            return
        }

        val name = abc(graph, closestNode)

        val areaName = "Goblin Holdout"

        ChatUtils.chat("first: $name")
        val start = SimpleTimeMark.now()
        val found = mutableListOf<GraphNode>()
        for (node in graph) {
            if (abc(graph, node) == "Goblin Holdout") {
                found.add(node)
            }
        }
        println(    "done checking nodes in ${start.passedSince()}")
        if (found.isEmpty()) {
            ChatUtils.userError("No $areaName nodes found!")
            return
        }
        ChatUtils.chat("start navigating to all ${found.size} nodes of $areaName")

        NavigateAllApi.navigateAll(
            found,
            "lol",
            LorenzColor.LIGHT_PURPLE.toColor(),
            onFinish = {
                ChatUtils.chat("done")
            },
            continueNavigationCondition = NavigationCondition.None,
            condition = { true },
        )
    }

    private fun abc(
        graph: Graph,
        closestNode: GraphNode,
    ): String? {
        val (_, map) = GraphUtils.findFastestPaths(graph, closestNode) { it.getAreaTag() != null }
        val seenAreas = mutableSetOf<String>()
        val sorted = map.sorted()
        val areaNodes = sorted.mapNotNull { (node, distance) ->
            val name = node.name?.takeIf { it !in seenAreas } ?: return@mapNotNull null
            val tag = node.getAreaTag() ?: return@mapNotNull null
            seenAreas += name
            AreaNode(node, name, tag, distance)
        }
        return areaNodes.firstOrNull()?.let {
            it.name
        }
    }
}
