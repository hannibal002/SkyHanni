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

/**
 * Trying to find errors in Area Graph for the current graph editor instance
 */
@SkyHanniModule
object GraphEditorBugFinder {

    private const val ERROR_LINE_HEIGHT = 10f

    private var errorsInWorld = emptyMap<GraphNode, Set<String>>()

    /** Collects all errors found during a single test run. A node can have more than one error. */
    private class BugCollector {
        val errors = mutableMapOf<GraphNode, MutableSet<String>>()

        fun add(node: GraphNode, error: String) {
            errors.getOrPut(node) { mutableSetOf() }.add(error)
        }
    }

    fun runTests() {
        CoroutineSettings("graph editor bug finder").launchCoroutine {
            asyncTest()
        }
    }

    private fun asyncTest() {
        val graph = IslandGraphs.currentIslandGraph ?: return
        val bugs = BugCollector()

        checkConflictingTags(graph, bugs)
        checkConflictingAreas(graph, bugs)
        checkMissingData(graph, bugs)
        checkDeprecatedTags(graph, bugs)
        checkInvalidNames(graph, bugs)
        checkHasSpawn(graph)
        checkOneWayEdges(graph, bugs)

        errorsInWorld = bugs.errors
        bugs.errors.keys.minByOrNull {
            it.distanceSqToPlayer()
        }?.pathFind("Graph Editor Bug", Color.RED, condition = { isEnabled() })
    }

    private fun checkDeprecatedTags(graph: Graph, bugs: BugCollector) {
        for (node in graph) {
            @Suppress("DEPRECATION")
            if (node.hasTag(GraphNodeTag.TELEPORT)) {
                bugs.add(node, "deprecated teleport node")
            }
        }
    }

    private fun checkInvalidNames(graph: Graph, bugs: BugCollector) {
        for (node in graph) {
            val name = node.name ?: continue
            if (node.hasTag(GraphNodeTag.WARP)) {
                if (!name.startsWith("/")) {
                    bugs.add(node, "invalid warp name")
                }
            }
            if (node.hasTag(GraphNodeTag.JUMP_PAD)) {
                if (IslandType.entries.none { it.name == name }) {
                    bugs.add(node, "jump pad name is no known island name")
                }
                if (name == SkyBlockUtils.currentIsland.name) {
                    bugs.add(node, "jump pad name is current island name")
                }
            }
        }
    }

    private fun checkHasSpawn(graph: Graph) {
        if (graph.none { it.hasTag(GraphNodeTag.POI) && it.name == "Spawn" }) {
            ChatUtils.chat("§cGraph editor without spawn point!")
        }
    }

    private fun checkMissingData(graph: Graph, bugs: BugCollector) {
        for (node in graph) {
            val nameNull = node.name.isNullOrBlank()
            val tagsEmpty = node.tags.isEmpty()
            if (nameNull > tagsEmpty) {
                bugs.add(node, "Missing name despite having tags")
            }
            if (tagsEmpty > nameNull) {
                bugs.add(node, "Missing tags despite having name")
            }
        }
    }

    private fun checkConflictingAreas(graph: Graph, bugs: BugCollector) {
        val nearestArea = mutableMapOf<GraphNode, GraphNode>()
        for (node in graph) {
            val pathToNearestArea = GraphUtils.findFastestPath(node) { it.getAreaTag() != null }?.first
            if (pathToNearestArea == null) {
                continue
            }
            val areaNode = pathToNearestArea.lastOrNull() ?: error("Empty path to the nearest area")
            nearestArea[node] = areaNode
        }
        for (node in graph) {
            val areaNode = nearestArea[node]?.name ?: continue
            for (neighbor in node.neighbors.keys) {
                val neighboringAreaNode = nearestArea[neighbor]?.name ?: continue
                if (neighboringAreaNode == areaNode) continue
                if ((null == node.getAreaTag())) {
                    bugs.add(node, "Conflicting areas $areaNode and $neighboringAreaNode")
                }
            }
        }
    }

    private fun checkConflictingTags(graph: Graph, bugs: BugCollector) {
        for (node in graph) {
            if (!node.tags.any { it in NavigationHelper.allowedTags }) continue
            val remainingTags = node.tags.filter { it in NavigationHelper.allowedTags }
            if (remainingTags.size != 1) {
                bugs.add(node, "Conflicting tags: $remainingTags")
            }
            if (node.hasTag(GraphNodeTag.MINES_EMISSARY)) {
                if (!node.hasTag(GraphNodeTag.NPC)) {
                    bugs.add(node, "emissary without npc tag")
                }
            }
        }
    }

    private fun checkOneWayEdges(graph: Graph, bugs: BugCollector) {
        for (node in graph) {
            for (neighbor in node.neighbors.keys) {
                if (node in neighbor.neighbors) continue
                bugs.add(node, "one-way edge starts here")
                bugs.add(neighbor, "one-way edge ends here (no way back)")
            }
        }
    }

    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        for ((node, texts) in errorsInWorld) {
            for ((index, text) in texts.withIndex()) {
                event.drawDynamicText(node.position, text, 1.5, yOff = index * ERROR_LINE_HEIGHT)
            }
        }
    }

    fun isEnabled() = GraphEditor.isEnabled()
}
