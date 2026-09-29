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
object GraphEditorErrorFinder {

    private const val ERROR_LINE_HEIGHT = 10f
    private const val MAX_RENDERED_NODES = 10

    private var errorsInWorld = emptyMap<GraphNode, Set<String>>()
    private var renderedErrors = emptyList<Pair<GraphNode, Set<String>>>()
    private var hasErrors = false

    private enum class ErrorCategory(val displayName: String) {
        CONFLICTING_TAGS("conflicting tags"),
        CONFLICTING_AREAS("conflicting areas"),
        MISSING_DATA("missing data"),
        DEPRECATED_TAGS("deprecated tags"),
        INVALID_NAMES("invalid names"),
        ONE_WAY_EDGES("one-way edges"),
    }

    /** Collects all errors found during a single test run. A node can have more than one error. */
    private class ErrorCollector {
        val errors = mutableMapOf<GraphNode, MutableSet<String>>()
        val categoryCounts = mutableMapOf<ErrorCategory, Int>()

        val totalErrors get() = categoryCounts.values.sum()

        var foundError = false

        /** Duplicate messages on the same node are dropped and therefore not counted. */
        fun add(node: GraphNode, category: ErrorCategory, error: String) {
            if (!errors.getOrPut(node) { mutableSetOf() }.add(error)) return
            categoryCounts.addOrPut(category, 1)
            foundError = true
        }
    }

    fun runTests() {
        CoroutineSettings("graph editor error finder").launchCoroutine {
            asyncTest()
        }
    }

    private fun asyncTest() {
        val graph = IslandGraphs.currentIslandGraph ?: return
        val errors = ErrorCollector()
        checkConflictingTags(graph, errors)
        checkConflictingAreas(graph, errors)
        checkMissingData(graph, errors)
        checkDeprecatedTags(graph, errors)
        checkInvalidNames(graph, errors)
        checkHasSpawn(graph, errors)
        checkOneWayEdges(graph, errors)

        errorsInWorld = errors.errors
        updateRenderedErrors()
        sendErrorsInChat(errors)
        errors.errors.keys.minByOrNull {
            it.distanceSqToPlayer()
        }?.pathFind("Graph Editor Error", Color.RED, condition = { isEnabled() && errorsInWorld.isNotEmpty() })
    }

    private fun sendErrorsInChat(errors: ErrorCollector) {
        if (!errors.foundError) {
            if (hasErrors) {
                ChatUtils.chat("§aGraph Editor errors are now gone.")
                hasErrors = false
            }
            return
        }
        hasErrors = true
        if (errors.errors.isNotEmpty()) {
            val breakdown = errors.categoryCounts.entries.sortedByDescending { it.value }
                .joinToString("\n") { (category, count) -> " §7${category.displayName}: §e${count.addSeparators()}" }
            val totalErrorCount = errors.totalErrors.addSeparators()
            val nodeCount = errors.errors.size.addSeparators()
            ChatUtils.chat("§cGraph Editor errors: $totalErrorCount on $nodeCount nodes\n$breakdown")
        }
    }

    private fun checkDeprecatedTags(graph: Graph, errors: ErrorCollector) {
        for (node in graph) {
            @Suppress("DEPRECATION")
            if (node.hasTag(GraphNodeTag.TELEPORT)) {
                errors.add(node, ErrorCategory.DEPRECATED_TAGS, "deprecated teleport node")
            }
        }
    }

    private fun checkInvalidNames(graph: Graph, errors: ErrorCollector) {
        for (node in graph) {
            val name = node.name ?: continue
            if (node.hasTag(GraphNodeTag.WARP)) {
                if (!name.startsWith("/")) {
                    errors.add(node, ErrorCategory.INVALID_NAMES, "invalid warp name")
                }
            }
            if (node.hasTag(GraphNodeTag.JUMP_PAD)) {
                if (IslandType.entries.none { it.name == name }) {
                    errors.add(node, ErrorCategory.INVALID_NAMES, "jump pad name is no known island name")
                }
                if (name == SkyBlockUtils.currentIsland.name) {
                    errors.add(node, ErrorCategory.INVALID_NAMES, "jump pad name is current island name")
                }
            }
        }
    }

    private fun checkHasSpawn(graph: Graph, errors: ErrorCollector) {
        if (graph.none { it.hasTag(GraphNodeTag.POI) && it.name == "Spawn" }) {
            ChatUtils.chat("§cGraph Editor without spawn point!")
            errors.foundError = true
        }
    }

    private fun checkMissingData(graph: Graph, errors: ErrorCollector) {
        for (node in graph) {
            val nameNull = node.name.isNullOrBlank()
            val tagsEmpty = node.tags.isEmpty()
            if (nameNull > tagsEmpty) {
                errors.add(node, ErrorCategory.MISSING_DATA, "Missing name despite having tags")
            }
            if (tagsEmpty > nameNull) {
                errors.add(node, ErrorCategory.MISSING_DATA, "Missing tags despite having name")
            }
        }
    }

    private fun checkConflictingAreas(graph: Graph, errors: ErrorCollector) {
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
                    errors.add(node, ErrorCategory.CONFLICTING_AREAS, "Conflicting areas $areaNode and $neighboringAreaNode")
                }
            }
        }
    }

    private fun checkConflictingTags(graph: Graph, errors: ErrorCollector) {
        for (node in graph) {
            if (!node.tags.any { it in NavigationHelper.allowedSingleNavigationTags }) continue
            val remainingTags = node.tags.filter { it in NavigationHelper.allowedSingleNavigationTags }
            if (remainingTags.size != 1) {
                errors.add(node, ErrorCategory.CONFLICTING_TAGS, "Conflicting tags: $remainingTags")
            }
            if (node.hasTag(GraphNodeTag.MINES_EMISSARY)) {
                if (!node.hasTag(GraphNodeTag.NPC)) {
                    errors.add(node, ErrorCategory.CONFLICTING_TAGS, "emissary without npc tag")
                }
            }
        }
    }

    private fun checkOneWayEdges(graph: Graph, errors: ErrorCollector) {
        for (node in graph) {
            for (neighbor in node.neighbors.keys) {
                if (hasPathBack(start = neighbor, target = node)) continue

                errors.add(node, ErrorCategory.ONE_WAY_EDGES, "one-way edge starts here")
                errors.add(neighbor, ErrorCategory.ONE_WAY_EDGES, "one-way edge ends here (no way back)")
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
