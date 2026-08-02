package at.hannibal2.skyhanni.utils.navigation

import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils

object NavigationUtils {

    fun getRoute(
        nodes: List<GraphNode>,
        maxIterations: Int = 300,
        neighborhoodSize: Int = 50,
    ): List<LorenzVec> {
        if (nodes.isEmpty()) return emptyList()
        val graph = IslandGraphs.currentIslandGraph ?: error("no active island graph")

        val closestNode = graph.minByOrNull { it.position.distanceSqToPlayer() } ?: error("no closest node")
        val route = calculateTravelingSalesman(nodes, closestNode, maxIterations, neighborhoodSize)

        val closestNodeIsTarget = nodes.any { it.position == closestNode.position }
        val amountOffset = if (closestNodeIsTarget) 0 else 1

        if (route.size != nodes.size + amountOffset) {
            ErrorManager.skyHanniError(
                "calculateTravelingSalesman could not reach all goals",
                "targetNodes" to nodes.size,
                "output" to route.size,
                "island" to SkyBlockUtils.currentIsland,
            )
        }

        return route.drop(amountOffset).map { it.position }
    }

    private fun calculateTravelingSalesman(
        nodes: List<GraphNode>,
        startNode: GraphNode,
        maxIterations: Int,
        neighborhoodSize: Int,
    ): List<GraphNode> {
        val distanceMap = computeDistanceMap(nodes, startNode)

        val route = directedTwoOpt(
            greedyTSP(distanceMap, startNode),
            distanceMap,
            maxIterations,
            neighborhoodSize,
        )

        ChatUtils.debug("Total distance of route: ${calculateRouteDistance(route, distanceMap).toInt()}")

        return route
    }

    private fun computeDistanceMap(
        nodes: List<GraphNode>,
        startNode: GraphNode,
    ): Map<GraphNode, Map<GraphNode, Double>> {
        val result = HashMap<GraphNode, Map<GraphNode, Double>>(nodes.size)

        for (node in nodes + startNode) {
            val distances = GraphUtils.findAllShortestDistances(node).distances

            result[node] = nodes.associateWith { distances[it] ?: Double.POSITIVE_INFINITY }
        }

        return result
    }

    private fun greedyTSP(
        distanceMap: Map<GraphNode, Map<GraphNode, Double>>,
        start: GraphNode,
    ): MutableList<GraphNode> {
        val route = mutableListOf(start)
        val visited = hashSetOf(start)

        var current = start

        while (visited.size < distanceMap.size) {
            val next = distanceMap[current]?.filterKeys { it !in visited }?.minByOrNull { it.value }?.key ?: break

            route += next
            visited += next
            current = next
        }

        return route
    }

    private fun directedTwoOpt(
        routeInput: MutableList<GraphNode>,
        distanceMap: Map<GraphNode, Map<GraphNode, Double>>,
        maxIterations: Int,
        neighborhoodSize: Int,
    ): MutableList<GraphNode> {
        val route = routeInput.toMutableList()

        var improved = true
        var iteration = 0

        while (improved && iteration < maxIterations) {
            improved = false

            for (i in 1 until route.size - 2) {
                val maxJ = minOf(route.size - 2, i + neighborhoodSize)

                for (j in i + 1..maxJ) {
                    val oldCost = edge(route[i - 1], route[i], distanceMap) + edge(route[j], route[j + 1], distanceMap)
                    val newCost = edge(route[i - 1], route[j], distanceMap) + edge(route[i], route[j + 1], distanceMap)

                    if (newCost < oldCost) {
                        route.subList(i, j + 1).reverse()
                        improved = true
                        break
                    }
                }

                if (improved) break
            }

            iteration++
        }

        return route
    }

    private fun edge(
        a: GraphNode?,
        b: GraphNode?,
        map: Map<GraphNode, Map<GraphNode, Double>>,
    ): Double {
        if (a == null || b == null) return 0.0
        return map[a]?.get(b) ?: Double.POSITIVE_INFINITY
    }

    private fun calculateRouteDistance(
        route: List<GraphNode>,
        distanceMap: Map<GraphNode, Map<GraphNode, Double>>,
    ): Double {
        var total = 0.0

        for (i in 0 until route.lastIndex) {
            val distance = distanceMap[route[i]]?.get(route[i + 1]) ?: Double.POSITIVE_INFINITY

            if (!distance.isFinite()) {
                ChatUtils.debug(
                    "Missing route between ${route[i].position} and ${route[i + 1].position}",
                )
            }

            total += distance
        }

        return total
    }
}
