package at.hannibal2.skyhanni.utils.navigation

import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils

object NavigationUtils {

    fun getRoute(
        input: List<GraphNode>,
        maxIterations: Int = 50,
        neighborhoodSize: Int = 8,
    ): List<LorenzVec> {
        if (input.isEmpty()) return emptyList()

        val route = calculateTravelingSalesman(input, maxIterations, neighborhoodSize)

        if (route.size != input.size) {
            ErrorManager.skyHanniError(
                "calculateTravelingSalesman could not reach all goals",
                "input" to input.size,
                "output" to route.size,
                "island" to SkyBlockUtils.currentIsland,
            )
        }

        return route.map { it.position }
    }

    private fun calculateTravelingSalesman(
        nodes: List<GraphNode>,
        maxIterations: Int,
        neighborhoodSize: Int,
    ): List<GraphNode> {
        val distanceMap = computeDistanceMap(nodes)

        val route = directedTwoOpt(greedyTSP(distanceMap), distanceMap, maxIterations, neighborhoodSize)

        ChatUtils.debug("Total distance of graph: ${calculateRouteDistance(route, distanceMap)}")

        return adjustRouteForCurrentLocation(route, LocationUtils.playerLocation())
    }

    private fun computeDistanceMap(
        nodes: List<GraphNode>,
    ): Map<GraphNode, Map<GraphNode, Double>> {
        val result = HashMap<GraphNode, Map<GraphNode, Double>>(nodes.size)

        for (node in nodes) {
            val distances = GraphUtils.findAllShortestDistances(node).distances

            result[node] = nodes.associateWith {
                distances[it] ?: Double.POSITIVE_INFINITY
            }
        }

        return result
    }

    private fun greedyTSP(
        distanceMap: Map<GraphNode, Map<GraphNode, Double>>,
    ): MutableList<GraphNode> {
        val start = distanceMap.keys.first()

        val route = mutableListOf(start)
        val visited = HashSet<GraphNode>()
        visited.add(start)

        var current = start

        while (visited.size < distanceMap.size) {
            val next = distanceMap[current]
                ?.filter { !visited.contains(it.key) }
                ?.minByOrNull { it.value }
                ?.key
                ?: break

            route.add(next)
            visited.add(next)
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
                val maxJ = minOf(route.size - 1, i + neighborhoodSize)

                for (j in i + 1..maxJ) {
                    val oldCost = edge(route[i - 1], route[i], distanceMap) + edge(route[j], route.getOrNull(j + 1), distanceMap)
                    val newCost = edge(route[i - 1], route[j], distanceMap) + edge(route[i], route.getOrNull(j + 1), distanceMap)

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
                ChatUtils.debug("Missing route between ${route[i].position} and ${route[i + 1].position}")
            }

            total += distance
        }

        return total
    }

    private fun adjustRouteForCurrentLocation(
        route: List<GraphNode>,
        currentLocation: LorenzVec,
    ): List<GraphNode> {
        val closest = route.minByOrNull { it.position.distanceSq(currentLocation) } ?: return route

        val index = route.indexOf(closest)

        return route.drop(index) + route.take(index)
    }
}
