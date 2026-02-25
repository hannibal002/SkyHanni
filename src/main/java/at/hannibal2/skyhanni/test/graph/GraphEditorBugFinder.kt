package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.misc.pathfind.IslandAreaBackend.getAreaTag
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import java.awt.Color

// Trying to find errors in Area Graph for the current graph editor instance
@SkyHanniModule
object GraphEditorBugFinder {
    private var errorsInWorld = emptyMap<GraphNode, String>()

    fun runTests() {
        SkyHanniMod.launchCoroutine("graph editor bug finder") {
            asyncTest()
        }
    }

    private fun asyncTest() {
        val graph = IslandGraphs.currentIslandGraph ?: return
        val errorsInWorld: MutableMap<GraphNode, String> = mutableMapOf()

        checkConflictingTags(graph, errorsInWorld)
        checkConflictingAreas(graph, errorsInWorld)
        checkMissingData(graph, errorsInWorld)
        val clusters = checkDisjointClusters(graph, errorsInWorld)

        this.errorsInWorld = errorsInWorld
        if (clusters.size <= 1) {
            errorsInWorld.keys.minByOrNull {
                it.distanceSqToPlayer()
            }?.pathFind("Graph Editor Bug", Color.RED, condition = { isEnabled() })
        }
    }

    private fun checkDisjointClusters(graph: Graph, errorsInWorld: MutableMap<GraphNode, String>): List<Set<GraphNode>> {
        val clusters = GraphUtils.findDisjointClusters(graph)
        if (clusters.size <= 1) return clusters

        val closestCluster = clusters.minBy { cluster -> cluster.minOf { it.distanceSqToPlayer() } }
        val foreignClusters = clusters.filter { it !== closestCluster }
        val closestForeignNodes = foreignClusters.map { network -> network.minBy { it.distanceSqToPlayer() } }
        closestForeignNodes.forEach {
            errorsInWorld[it] = "§cDisjoint node network"
        }
        val closestForeignNode = closestForeignNodes.minBy { it.distanceSqToPlayer() }
        val closestNodeToForeignNode = closestCluster.minBy { it.position.distanceSq(closestForeignNode.position) }
        closestNodeToForeignNode.pathFind("Graph Editor Bug", Color.RED, condition = { isEnabled() })
        return clusters
    }

    private fun checkMissingData(graph: Graph, errorsInWorld: MutableMap<GraphNode, String>) {
        for (node in graph) {
            val nameNull = node.name.isNullOrBlank()
            val tagsEmpty = node.tags.isEmpty()
            if (nameNull > tagsEmpty) {
                errorsInWorld[node] = "§cMissing name despite having tags"
            }
            if (tagsEmpty > nameNull) {
                errorsInWorld[node] = "§cMissing tags despite having name"
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
            for (neighbour in node.neighbours.keys) {
                val neighbouringAreaNode = nearestArea[neighbour]?.name ?: continue
                if (neighbouringAreaNode == areaNode) continue
                if ((null == node.getAreaTag())) {
                    errorsInWorld[node] = "§cConflicting areas $areaNode and $neighbouringAreaNode"
                }
            }
        }
    }

    private fun checkConflictingTags(graph: Graph, errorsInWorld: MutableMap<GraphNode, String>) {
        for (node in graph) {
            if (!node.tags.any { it in NavigationHelper.allowedTags }) continue
            val remainingTags = node.tags.filter { it in NavigationHelper.allowedTags }
            if (remainingTags.size != 1) {
                errorsInWorld[node] = "§cConflicting tags: $remainingTags"
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
