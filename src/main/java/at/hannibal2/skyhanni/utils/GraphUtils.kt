package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.findShortestPathAsGraphWithDistance

object GraphUtils {

    fun findFastestPaths(
        graph: Graph,
        closedNote: GraphNode,
        condition: (GraphNode) -> Boolean = { true },
    ): Pair<MutableMap<GraphNode, Graph>, MutableMap<GraphNode, Double>> {
        val paths = mutableMapOf<GraphNode, Graph>()

        val map = mutableMapOf<GraphNode, Double>()
        for (graphNode in graph.nodes) {
            if (!condition(graphNode)) continue
            val (path, distance) = graph.findShortestPathAsGraphWithDistance(closedNote, graphNode)
            paths[graphNode] = path
            map[graphNode] = distance
        }
        return Pair(paths, map)
    }
}
