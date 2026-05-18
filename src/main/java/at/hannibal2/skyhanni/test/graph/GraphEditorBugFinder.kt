package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.misc.pathfind.IslandAreaBackend.getAreaTag
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import java.awt.Color

// Trying to find errors in Area Graph for the current graph editor instance
@SkyHanniModule
object GraphEditorBugFinder {
    private var errorsInWorld = emptyMap<GraphNode, String>()

    fun runTests() {
        CoroutineSettings("graph editor bug finder").launchCoroutine {
            asyncTest()
        }
    }

    private fun asyncTest() {
        val graph = IslandGraphs.currentIslandGraph ?: return
        val errorsInWorld: MutableMap<GraphNode, String> = mutableMapOf()

        checkConflictingTags(graph, errorsInWorld)
        checkConflictingAreas(graph, errorsInWorld)
        checkMissingData(graph, errorsInWorld)
        checkDeprecatedTags(graph, errorsInWorld)
        checkInvalidNames(graph, errorsInWorld)
        checkHasSpawn(graph, errorsInWorld)

        this.errorsInWorld = errorsInWorld
        errorsInWorld.keys.minByOrNull {
            it.distanceSqToPlayer()
        }?.pathFind("Graph Editor Bug", Color.RED, condition = { isEnabled() })
    }

    private fun checkDeprecatedTags(
        graph: Graph,
        errorsInWorld: MutableMap<GraphNode, String>,
    ) {
        for (node in graph) {
            @Suppress("DEPRECATION")
            if (node.hasTag(GraphNodeTag.TELEPORT)) {
                errorsInWorld[node] = "deprecated teleport node"
            }
        }
    }

    private fun checkInvalidNames(
        graph: Graph,
        errorsInWorld: MutableMap<GraphNode, String>,
    ) {
        for (node in graph) {
            val name = node.name ?: continue
            if (node.hasTag(GraphNodeTag.WARP)) {
                if (!name.startsWith("/")) {
                    errorsInWorld[node] = "invalid warp name"
                }
            }
            if (node.hasTag(GraphNodeTag.JUMP_PAD)) {
                if (IslandType.entries.none { it.name == name }) {
                    errorsInWorld[node] = "jump pad name is no known island name"
                }
                if (name == SkyBlockUtils.currentIsland.name) {
                    errorsInWorld[node] = "jump pad name is current island name"
                }
            }
        }
    }

    private fun checkHasSpawn(
        graph: Graph,
        errorsInWorld: MutableMap<GraphNode, String>,
    ) {
        if (graph.none { it.hasTag(GraphNodeTag.POI) && it.name == "Spawn" }) {
            ChatUtils.chat("§cGraph editor without spawn point!")
        }
    }

    private fun checkMissingData(graph: Graph, errorsInWorld: MutableMap<GraphNode, String>) {
        for (node in graph) {
            val nameNull = node.name.isNullOrBlank()
            val tagsEmpty = node.tags.isEmpty()
            if (nameNull > tagsEmpty) {
                errorsInWorld[node] = "Missing name despite having tags"
            }
            if (tagsEmpty > nameNull) {
                errorsInWorld[node] = "Missing tags despite having name"
            }
        }
    }

    private fun checkConflictingAreas(graph: Graph, errorsInWorld: MutableMap<GraphNode, String>) {
        val nearestArea = mutableMapOf<GraphNode, GraphNode>()
        for (node in graph) {
            val pathToNearestArea = GraphUtils.findFastestPath(node) { it.getAreaTag() != null }?.first
            if (pathToNearestArea == null) {
                continue
            }
            val areaNode = pathToNearestArea.lastOrNull() ?: error("Empty path to nearest area")
            nearestArea[node] = areaNode
        }
        for (node in graph) {
            val areaNode = nearestArea[node]?.name ?: continue
            for (neighbor in node.neighbors.keys) {
                val neighboringAreaNode = nearestArea[neighbor]?.name ?: continue
                if (neighboringAreaNode == areaNode) continue
                if ((null == node.getAreaTag())) {
                    errorsInWorld[node] = "Conflicting areas $areaNode and $neighboringAreaNode"
                }
            }
        }
    }

    private fun checkConflictingTags(graph: Graph, errorsInWorld: MutableMap<GraphNode, String>) {
        for (node in graph) {
            if (!node.tags.any { it in NavigationHelper.allowedTags }) continue
            val remainingTags = node.tags.filter { it in NavigationHelper.allowedTags }
            if (remainingTags.size != 1) {
                errorsInWorld[node] = "Conflicting tags: $remainingTags"
            }
            if (node.hasTag(GraphNodeTag.MINES_EMISSARY)) {
                if (!node.hasTag(GraphNodeTag.NPC)) {
                    errorsInWorld[node] = "emissary without npc tag"
                }
            }
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        for ((node, text) in errorsInWorld) {
            event.drawDynamicText(node.position, text, 1.5)
        }
    }

    fun isEnabled() = GraphEditor.isEnabled()
}
