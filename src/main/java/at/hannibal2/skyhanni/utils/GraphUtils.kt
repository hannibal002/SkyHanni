package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.findAllShortestDistances
import at.hannibal2.skyhanni.data.model.findDijkstraDistances
import at.hannibal2.skyhanni.data.model.findPathToDestination

object GraphUtils {
    /**
     * Find the fastest path from [closestNode] to *any* node that matches [condition].
     */
    fun findFastestPath(graph: Graph,
                        closestNode: GraphNode,
                        condition: (GraphNode) -> Boolean): Pair<Graph, Double>? {
        val distances = graph.findDijkstraDistances(closestNode, condition)
        val entry = distances.lastVisitedNode.takeIf(condition)
        return entry?.let {
            distances.findPathToDestination(it)
        }
    }

    /**
     * Find the fastest path from [closestNode] to *all* nodes that matches [condition].
     */
    fun findFastestPaths(
        graph: Graph,
        closestNode: GraphNode,
        condition: (GraphNode) -> Boolean = { true },
    ): Pair<MutableMap<GraphNode, Graph>, MutableMap<GraphNode, Double>> {
        val paths = mutableMapOf<GraphNode, Graph>()

        val map = mutableMapOf<GraphNode, Double>()
        val distances = graph.findAllShortestDistances(closestNode)
        for (graphNode in graph.nodes) {
            if (!condition(graphNode)) continue
            val (path, distance) = distances.findPathToDestination(graphNode)
            paths[graphNode] = path
            map[graphNode] = distance
        }
        return Pair(paths, map)
    }
}
