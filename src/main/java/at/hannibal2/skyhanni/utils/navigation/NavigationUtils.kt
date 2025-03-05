package at.hannibal2.skyhanni.utils.navigation

import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import kotlin.system.measureTimeMillis

object NavigationUtils {

    fun getRoute(targetNodes: List<GraphNode>): List<LorenzVec> {
        var distanceMap: Map<GraphNode, Map<GraphNode, Double>>
        val distanceMapTime = measureTimeMillis {
            distanceMap = computeDistanceMap(targetNodes)
        }
        println("computeDistanceMap took $distanceMapTime ms.")

        var tspRoute: List<GraphNode>
        val tspRouteTime = measureTimeMillis {
            tspRoute = improvedTSP(distanceMap)
        }
        println("improvedTSP took $tspRouteTime ms.")

        var currentPosition: LorenzVec
        val currentPositionTime = measureTimeMillis {
            currentPosition = LocationUtils.playerLocation()
        }
        println("LocationUtils.playerLocation took $currentPositionTime ms.")

        var adjustedRoute: List<GraphNode>
        val adjustRouteTime = measureTimeMillis {
            adjustedRoute = adjustRouteForCurrentLocation(tspRoute, currentPosition)
        }
        println("adjustRouteForCurrentLocation took $adjustRouteTime ms.")

        return adjustedRoute.map { it.position }
    }

    // Step 1: Preload the 50×50 Distance Matrix
    private fun computeDistanceMap(targetNodes: List<GraphNode>): Map<GraphNode, Map<GraphNode, Double>> {
        val distanceMap = mutableMapOf<GraphNode, MutableMap<GraphNode, Double>>()
        for (node in targetNodes) {
            val dijkstraTree = GraphUtils.findAllShortestDistances(node)
            val nodeDistances = mutableMapOf<GraphNode, Double>()
            for (target in targetNodes) {
                nodeDistances[target] = dijkstraTree.distances[target] ?: Double.POSITIVE_INFINITY
            }
            distanceMap[node] = nodeDistances
        }
        return distanceMap
    }

    // Improved TSP using Greedy initialization + 2-opt optimization
    private fun improvedTSP(distanceMap: Map<GraphNode, Map<GraphNode, Double>>): List<GraphNode> {
        // Step 1: Get initial route from the simple greedy algorithm.
        val route = greedyTSP(distanceMap).toMutableList()

        // Step 2: Apply 2-opt improvement with limits.
        var improved = true
        var iteration = 0
        val maxIterations = 50 // Cap on total iterations.
        val neighborhoodSize = 6 // Limit candidate j-range for each i.

        while (improved && iteration < maxIterations) {
            improved = false
            // Fix the starting node; begin at index 1.
            for (i in 1 until route.size - 1) {
                // Limit j to a smaller neighborhood.
                val jMax = (i + neighborhoodSize).coerceAtMost(route.size)
                for (j in i + 1 until jMax) {
                    val costCurrent = distanceMap.getValue(route[i - 1]).getValue(route[i]) +
                        distanceMap.getValue(route[j - 1]).getValue(route[j])
                    val costNew = distanceMap.getValue(route[i - 1]).getValue(route[j]) +
                        distanceMap.getValue(route[j - 1]).getValue(route[i])
                    if (costNew < costCurrent) {
                        route.subList(i, j).reverse()
                        improved = true
                    }
                }
            }
            iteration++
        }
        return route
    }

    // Step 2: Fast Greedy TSP Algorithm (~1ms for 50 nodes)
    private fun greedyTSP(distanceMap: Map<GraphNode, Map<GraphNode, Double>>): List<GraphNode> {
        val startNode = distanceMap.keys.first()
        val route = mutableListOf(startNode)
        val visited = mutableSetOf(startNode)
        var current = startNode

        while (visited.size < distanceMap.size) {
            var nextNode: GraphNode? = null
            var bestDistance = Double.POSITIVE_INFINITY

            // Try to pick the nearest unvisited neighbor from the current node.
            distanceMap[current]?.forEach { (candidate, distance) ->
                if (candidate !in visited && distance < bestDistance) {
                    bestDistance = distance
                    nextNode = candidate
                }
            }

            // If none was found, search among all unvisited nodes.
            if (nextNode == null) {
                for (candidate in distanceMap.keys.filter { it !in visited }) {
                    val candidateMinDistance =
                        visited.mapNotNull { distanceMap[it]?.get(candidate) }.minOrNull() ?: Double.POSITIVE_INFINITY
                    if (candidateMinDistance < bestDistance) {
                        bestDistance = candidateMinDistance
                        nextNode = candidate
                    }
                }
            }

            // Use a temporary variable for safe smart cast.
            val chosen = nextNode
            if (chosen != null) {
                route.add(chosen)
                visited.add(chosen)
                current = chosen
            } else {
                break
            }
        }
        return route
    }

    // Given: a TSP route (a list of target GraphNodes forming a cycle)
// and a current location (as a LorenzVec), plus a helper to compute distance.
    private fun adjustRouteForCurrentLocation(
        route: List<GraphNode>,
        currentLocation: LorenzVec,
    ): List<GraphNode> {
        // Find the closest node in the route by comparing the squared distances.
        val closestNode = route.minByOrNull { it.position.distanceSq(currentLocation) } ?: route.first()
        // Rotate the route so that the closest node comes first.
        val idx = route.indexOf(closestNode)
        return route.drop(idx) + route.take(idx)
    }
}
