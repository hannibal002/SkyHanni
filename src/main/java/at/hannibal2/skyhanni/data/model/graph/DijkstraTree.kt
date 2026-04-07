package at.hannibal2.skyhanni.data.model.graph

data class DijkstraTree(
    val origin: GraphNode,
    /**
     * A map of distances between the [origin] and each node in a graph. This distance map is only accurate for nodes
     * closer to the origin than the [lastVisitedNode]. In case there is no early bailout, this map will be accurate
     * for all nodes in the graph.
     */
    val distances: Map<GraphNode, Double>,
    /**
     * A map of nodes to the neighboring node that is the quickest path towards the origin (the neighboring node
     * that has the lowest value in [distances]).
     */
    val towardsOrigin: Map<GraphNode, GraphNode>,
    /**
     * Either the furthest away node in the graph, or the node that was bailed out on early because it fulfilled the
     * search condition. In case the search condition matches nothing, this will still be the furthest away node,
     * so an additional check may be necessary.
     */
    val lastVisitedNode: GraphNode,
)

@Suppress("MapGetWithNotNullAssertionOperator", "UnsafeCallOnNullableType")
fun DijkstraTree.findPathToDestination(end: GraphNode): Pair<Graph, Double> {
    val distances = this
    val reversePath = buildList {
        var current = end
        while (true) {
            add(current)
            if (current == origin) break
            current = towardsOrigin[current] ?: return Graph() to 0.0
        }
    }
    return Graph(reversePath.reversed()) to distances.distances[end]!!
}
