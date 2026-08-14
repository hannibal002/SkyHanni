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
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import java.awt.Color

/**
 * Trying to find errors in Area Graph for the current graph editor instance
 */
@SkyHanniModule
object GraphEditorBugFinder {

    private const val ERROR_LINE_HEIGHT = 10f
    private const val MAX_RENDERED_NODES = 10

    private var errorsInWorld = emptyMap<GraphNode, Set<String>>()
    private var renderedErrors = emptyList<Pair<GraphNode, Set<String>>>()

    private enum class BugCategory(val displayName: String) {
        CONFLICTING_TAGS("conflicting tags"),
        CONFLICTING_AREAS("conflicting areas"),
        MISSING_DATA("missing data"),
        DEPRECATED_TAGS("deprecated tags"),
        INVALID_NAMES("invalid names"),
        ONE_WAY_EDGES("one-way edges"),
    }

    /** Collects all errors found during a single test run. A node can have more than one error. */
    private class BugCollector {
        val errors = mutableMapOf<GraphNode, MutableSet<String>>()
        val categoryCounts = mutableMapOf<BugCategory, Int>()

        val totalErrors get() = categoryCounts.values.sum()

        /** Duplicate messages on the same node are dropped and therefore not counted. */
        fun add(node: GraphNode, category: BugCategory, error: String) {
            if (!errors.getOrPut(node) { mutableSetOf() }.add(error)) return
            categoryCounts.addOrPut(category, 1)
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
        updateRenderedErrors()
        reportBugCount(bugs)
        bugs.errors.keys.minByOrNull {
            it.distanceSqToPlayer()
        }?.pathFind("Graph Editor Bug", Color.RED, condition = { isEnabled() })
    }

    private fun reportBugCount(bugs: BugCollector) {
        val totalErrors = bugs.totalErrors
        if (totalErrors == 0) return
        val breakdown = bugs.categoryCounts.entries.sortedByDescending { it.value }
            .joinToString("\n") { (category, count) -> " §7${category.displayName}: §e${count.addSeparators()}" }
        ChatUtils.chat(
            "§cGraph errors: ${totalErrors.addSeparators()} on ${bugs.errors.size.addSeparators()} nodes\n$breakdown",
        )
    }

    private fun checkDeprecatedTags(graph: Graph, bugs: BugCollector) {
        for (node in graph) {
            @Suppress("DEPRECATION")
            if (node.hasTag(GraphNodeTag.TELEPORT)) {
                bugs.add(node, BugCategory.DEPRECATED_TAGS, "deprecated teleport node")
            }
        }
    }

    private fun checkInvalidNames(graph: Graph, bugs: BugCollector) {
        for (node in graph) {
            val name = node.name ?: continue
            if (node.hasTag(GraphNodeTag.WARP)) {
                if (!name.startsWith("/")) {
                    bugs.add(node, BugCategory.INVALID_NAMES, "invalid warp name")
                }
            }
            if (node.hasTag(GraphNodeTag.JUMP_PAD)) {
                if (IslandType.entries.none { it.name == name }) {
                    bugs.add(node, BugCategory.INVALID_NAMES, "jump pad name is no known island name")
                }
                if (name == SkyBlockUtils.currentIsland.name) {
                    bugs.add(node, BugCategory.INVALID_NAMES, "jump pad name is current island name")
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
                bugs.add(node, BugCategory.MISSING_DATA, "Missing name despite having tags")
            }
            if (tagsEmpty > nameNull) {
                bugs.add(node, BugCategory.MISSING_DATA, "Missing tags despite having name")
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
                    bugs.add(node, BugCategory.CONFLICTING_AREAS, "Conflicting areas $areaNode and $neighboringAreaNode")
                }
            }
        }
    }

    private fun checkConflictingTags(graph: Graph, bugs: BugCollector) {
        for (node in graph) {
            if (!node.tags.any { it in NavigationHelper.allowedSingleNavigationTags }) continue
            val remainingTags = node.tags.filter { it in NavigationHelper.allowedSingleNavigationTags }
            if (remainingTags.size != 1) {
                bugs.add(node, BugCategory.CONFLICTING_TAGS, "Conflicting tags: $remainingTags")
            }
            if (node.hasTag(GraphNodeTag.MINES_EMISSARY)) {
                if (!node.hasTag(GraphNodeTag.NPC)) {
                    bugs.add(node, BugCategory.CONFLICTING_TAGS, "emissary without npc tag")
                }
            }
        }
    }

    private fun checkOneWayEdges(graph: Graph, bugs: BugCollector) {
        for (node in graph) {
            for (neighbor in node.neighbors.keys) {
                if (hasPathBack(start = neighbor, target = node)) continue

                bugs.add(node, BugCategory.ONE_WAY_EDGES, "one-way edge starts here")
                bugs.add(neighbor, BugCategory.ONE_WAY_EDGES, "one-way edge ends here (no way back)")
            }
        }
    }

    private fun hasPathBack(start: GraphNode, target: GraphNode): Boolean {
        val queue = ArrayDeque<GraphNode>().apply { add(start) }
        val visited = mutableSetOf(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == target) return true

            for (next in current.neighbors.keys) {
                if (visited.add(next)) queue.add(next)
            }
        }
        return false
    }

    @HandleEvent
    private fun onSecondPassed() {
        if (!isEnabled()) return
        updateRenderedErrors()
    }

    /** Only the closest nodes are rendered, so a graph with many errors stays readable. */
    private fun updateRenderedErrors() {
        renderedErrors = errorsInWorld.entries
            .sortedBy { it.key.distanceSqToPlayer() }
            .take(MAX_RENDERED_NODES)
            .map { it.toPair() }
    }

    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        for ((node, texts) in renderedErrors) {
            for ((index, text) in texts.withIndex()) {
                event.drawDynamicText(node.position, text, 1.5, yOff = index * ERROR_LINE_HEIGHT)
            }
        }
    }

    fun isEnabled() = GraphEditor.isEnabled()
}
