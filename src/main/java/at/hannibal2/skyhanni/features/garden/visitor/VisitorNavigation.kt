package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.WarpApi
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenVisitor
import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.features.misc.pathfind.NavigateAllApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.contains

@SkyHanniModule
object VisitorNavigation {
    private data class VisitorNavigationData(
        val island: IslandType,
        val position: LorenzVec,
        val name: String,
    )

    private var visitors = mapOf<IslandType, List<VisitorNavigationData>>()
    private var noPositionVisitors = setOf<String>()

    private val currentIslandVisitors get() = visitors[SkyBlockUtils.currentIsland].orEmpty()

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        val visitors = event.getConstant<GardenJson>("Garden").visitors
        loadVisitors(visitors)
    }

    private fun loadVisitors(visitorsJson: Map<String, GardenVisitor>) {
        val otherVisitors = mutableSetOf<String>()
        val visitorsByIsland = visitorsJson.entries
            .groupBy { it.value.mode }
            .mapNotNull { (mode, visitors) ->
                val island = mode?.let(IslandType::getByIdOrNull) ?: run {
                    otherVisitors += visitors.map { it.key }
                    return@mapNotNull null
                }
                island to visitors
            }

        this.visitors = visitorsByIsland.associate { (island, visitors) ->
            val navigationData = visitors.mapNotNull { (name, visitor) ->
                val position = visitor.position ?: run {
                    otherVisitors += name
                    return@mapNotNull null
                }
                VisitorNavigationData(
                    island = island,
                    position = position,
                    name = name,
                )
            }

            island to navigationData
        }

        noPositionVisitors = otherVisitors
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shvisitornav") {
            description = "Navigates to visitors either on the current island or other islands, if they have a fixed location"
            category = USERS_ACTIVE

            coroutineArgCallback(
                "visitor",
                BrigadierArguments.greedyString(),
                BrigadierUtils.dynamicSuggestionProvider { currentIslandVisitors.map { it.name } },
            ) { name ->
                val visitor = currentIslandVisitors.firstOrNull {
                    it.name.equals(name, ignoreCase = true)
                }

                if (visitor == null) {
                    visitorNotFound(name)
                    return@coroutineArgCallback
                }

                startNavigation(visitor)
            }
        }

        event.registerBrigadier("shvisitornavall") {
            description = "Navigates to all visitors on the current island"
            category = USERS_ACTIVE

            coroutineSimpleCallback {
                startAllNavigation()
            }
        }
    }

    private fun visitorNotFound(rawName: String) {
        val visitor = visitors.values
            .flatten()
            .firstOrNull { it.name.equals(rawName, ignoreCase = true) }

        if (visitor == null) {
            if (!noPositionVisitors.contains(rawName, ignoreCase = true)) {
                ChatUtils.userError("Visitor §a'$rawName' §ccould not be found.")
                return
            }
            ChatUtils.userError(
                "Visitor §a'$rawName' §cdoes not have a fixed location. " +
                    "Some visitors only appear under specific conditions."
            )
            WikiManager.sendWikiMessage(rawName, autoOpen = false)
            return
        }

        ChatUtils.chat(
            "§7Visitor §a'${visitor.name}' §7is at §a${visitor.island.displayName}§7."
        )
        WarpApi.sendWarpMessage(
            position = visitor.position,
            island = visitor.island,
            shouldRetry = true,
            onWarp = {
                startNavigation(visitor)
            },
            onFail = {
                ChatUtils.chat(
                    "§7Could not find a working warp to §a'${visitor.name}'§7."
                )
            },
        )
    }

    private fun startAllNavigation() {
        val graph = IslandGraphs.currentIslandGraph ?: return
        val npcNodes = graph.getNodesWithTags(NPC)
        val nodes = currentIslandVisitors.map { visitor ->
            getClosestNode(graph, npcNodes, visitor)
        }

        NavigateAllApi.navigateAll(
            nodes,
            targetName = "Visitor",
            color = LorenzColor.DARK_PURPLE.toColor(),
            onFinish = {
                ChatUtils.chat(
                    "Reached all ${StringUtils.pluralize(nodes.size, "§aVisitor", withNumber = true)}§e."
                )
            },
            continueNavigationCondition = None,
            condition = { true },
        )
    }

    private fun startNavigation(visitor: VisitorNavigationData) {
        val graph = IslandGraphs.currentIslandGraph ?: return
        val npcNodes = graph.getNodesWithTags(NPC)
        val node = getClosestNode(graph, npcNodes, visitor)
        node.pathFind(
            label = visitor.name,
            color = LorenzColor.DARK_PURPLE.toColor(),
            condition = { true },
        )
    }

    private fun getClosestNode(graph: Graph, candidateNodes: List<GraphNode>, visitor: VisitorNavigationData): GraphNode {
        return candidateNodes.filter { it.name == visitor.name }
            .minByOrNull { it.position.distanceSq(visitor.position) }
            ?: graph.getNearestNode(visitor.position)
    }
}
