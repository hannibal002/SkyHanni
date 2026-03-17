package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import kotlin.math.sqrt

object GraphEditorNetworks {

    private val networkColors = listOf(
        LorenzColor.GOLD,
        LorenzColor.GREEN,
        LorenzColor.AQUA,
        LorenzColor.LIGHT_PURPLE,
        LorenzColor.WHITE,
        LorenzColor.DARK_GREEN,
    )

    fun recalculate(): Int {
        val state = GraphEditor.state
        val clusters = findClusters(state.nodes, state.edges)
        val useNetworkColors = clusters.size > 1

        if (!useNetworkColors) {
            for (edge in state.edges) {
                edge.networkColor = null
            }
            return clusters.size
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
            edge.networkColor = networkColors[colorIndex].addOpacity(150)
        }

        return clusters.size
    }

    fun copyClosestNetwork() {
        val state = GraphEditor.state
        val closestNode = state.closestNode
        if (closestNode == null) {
            ChatUtils.userError("No nearby node found!")
            return
        }

        val adjacency = buildAdjacency(state.nodes, state.edges)
        val cluster = bfs(closestNode, adjacency)

        val clusterNodes = state.nodes.filter { it in cluster }.toSet()
        val graph = GraphEditorIO.compileGraph(nodeSubset = clusterNodes)
        val json = graph.toJson()
        OSUtils.copyToClipboard(json)

        val nodeCount = clusterNodes.size.addSeparators()
        val edgeCount = state.edges.count { it.node1 in cluster && it.node2 in cluster }.addSeparators()
        ChatUtils.chat("Copied network with $nodeCount nodes and $edgeCount edges to clipboard.")
    }

    fun findNetworks() {
        val state = GraphEditor.state
        val clusters = findClusters(state.nodes, state.edges)

        if (clusters.isEmpty()) {
            ChatUtils.chat("No networks found.")
            return
        }

        if (clusters.size == 1) {
            val nodeCount = state.nodes.size.addSeparators()
            ChatUtils.chat("Graph is fully connected (1 network, $nodeCount nodes).")
            return
        }

        val sortedClusters = clusters.sortedByDescending { it.size }

        ChatUtils.chat("§eGraph Networks: ${clusters.size} found")
        for ((index, cluster) in sortedClusters.withIndex()) {
            val colorIndex = index % networkColors.size
            val color = networkColors[colorIndex]
            val chatColor = color.getChatColor()
            val nodeCount = cluster.size.addSeparators()
            val nearestNode = cluster.minByOrNull { it.distanceSqToPlayer() } ?: continue
            val distance = sqrt(nearestNode.distanceSqToPlayer()).toInt()

            ChatUtils.clickableChat(
                "$chatColor■ §eNetwork ${index + 1}: $nodeCount nodes (nearest: $distance blocks)",
                onClick = {
                    IslandGraphs.pathFind(
                        nearestNode.position,
                        "Network ${index + 1}",
                        color.toColor(),
                        condition = { GraphEditor.isEnabled() },
                    )
                },
                hover = "Click to navigate! (requires Save with 'use as island area')",
            )
        }
    }

    fun bridgeNetworks(graph: Graph): Graph {
        val clusters = GraphUtils.findDisjointClusters(graph).map { it.toMutableSet() }.toMutableList()

        while (clusters.size > 1) {
            var bestDistanceSq = Double.MAX_VALUE
            var bestNodeA: GraphNode? = null
            var bestNodeB: GraphNode? = null
            var bestIndexA = -1
            var bestIndexB = -1

            for (i in clusters.indices) {
                for (j in i + 1 until clusters.size) {
                    for (nodeA in clusters[i]) {
                        for (nodeB in clusters[j]) {
                            val distSq = nodeA.position.distanceSq(nodeB.position)
                            if (distSq < bestDistanceSq) {
                                bestDistanceSq = distSq
                                bestNodeA = nodeA
                                bestNodeB = nodeB
                                bestIndexA = i
                                bestIndexB = j
                            }
                        }
                    }
                }
            }

            val nodeA = bestNodeA ?: break
            val nodeB = bestNodeB ?: break
            val distance = nodeA.position.distance(nodeB.position)

            nodeA.neighbours += (nodeB to distance)
            nodeB.neighbours += (nodeA to distance)

            clusters[bestIndexA].addAll(clusters[bestIndexB])
            clusters.removeAt(bestIndexB)
        }

        return graph
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
