package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.utils.LorenzColor

object GraphEditorNetworks {

    private val networkColors = listOf(
        LorenzColor.GOLD.addOpacity(150),
        LorenzColor.GREEN.addOpacity(150),
        LorenzColor.AQUA.addOpacity(150),
        LorenzColor.LIGHT_PURPLE.addOpacity(150),
        LorenzColor.WHITE.addOpacity(150),
        LorenzColor.DARK_GREEN.addOpacity(150),
    )

    fun recalculate() {
        val state = GraphEditor.state
        val clusters = findClusters(state.nodes, state.edges)
        val useNetworkColors = clusters.size > 1

        if (!useNetworkColors) {
            for (edge in state.edges) {
                edge.networkColor = null
            }
            return
        }

        val sortedClusters = clusters.sortedByDescending { it.size }
        val nodeToColorIndex = mutableMapOf<GraphingNode, Int>()
        for ((index, cluster) in sortedClusters.withIndex()) {
            val colorIndex = index % networkColors.size
            for (node in cluster) {
                nodeToColorIndex[node] = colorIndex
            }
        }

        for (edge in state.edges) {
            val colorIndex = nodeToColorIndex[edge.node1] ?: 0
            edge.networkColor = networkColors[colorIndex]
        }
    }

    private fun findClusters(
        nodes: List<GraphingNode>,
        edges: List<GraphingEdge>,
    ): List<Set<GraphingNode>> {
        val adjacency = buildAdjacency(nodes, edges)
        val visited = mutableSetOf<GraphingNode>()
        val clusters = mutableListOf<Set<GraphingNode>>()

        for (node in nodes) {
            if (node in visited) continue
            val cluster = bfs(node, adjacency)
            visited.addAll(cluster)
            clusters.add(cluster)
        }

        return clusters
    }

    private fun buildAdjacency(
        nodes: List<GraphingNode>,
        edges: List<GraphingEdge>,
    ): Map<GraphingNode, List<GraphingNode>> {
        val adjacency = nodes.associateWith { mutableListOf<GraphingNode>() }
        for (edge in edges) {
            adjacency[edge.node1]?.add(edge.node2)
            adjacency[edge.node2]?.add(edge.node1)
        }
        return adjacency
    }

    private fun bfs(
        start: GraphingNode,
        adjacency: Map<GraphingNode, List<GraphingNode>>,
    ): Set<GraphingNode> {
        val visited = mutableSetOf(start)
        val queue = ArrayDeque<GraphingNode>()
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (neighbor in adjacency[current].orEmpty()) {
                if (neighbor in visited) continue
                visited.add(neighbor)
                queue.add(neighbor)
            }
        }

        return visited
    }
}
