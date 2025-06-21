package at.hannibal2.skyhanni.utils.navigation

import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils

object NavigationUtils {

    fun getRoute(input: List<GraphNode>, maxIterations: Int = 50, neighborhoodSize: Int = 6): List<LorenzVec> {
        if (input.isEmpty()) return emptyList()
        val output = calculateTravelingSalesman(input, maxIterations, neighborhoodSize)

        if (input.size != output.size) {
            ErrorManager.skyHanniError(
                "calculateTravelingSalesman could not reach all goals",
                "input" to input.size,
                "output" to output.size,
                "island" to SkyBlockUtils.currentIsland,
            )
        }

        return output
    }

    private fun calculateTravelingSalesman(
        targetNodes: List<GraphNode>,
        maxIterations: Int,
        neighborhoodSize: Int,
    ): List<LorenzVec> {
        val distanceMap = computeDistanceMap(targetNodes)
        var tspRoute = improvedTSP(distanceMap, maxIterations, neighborhoodSize)

        optimizeCriticalSegments(tspRoute, distanceMap)

        // Re-run TSP with an optimized (intra- and inter-cluster) distance map.
        val optimizedDistanceMap = computeDistanceMapOptimized(tspRoute)
        tspRoute = improvedTSP(optimizedDistanceMap, maxIterations, neighborhoodSize)

        // Apply 3‑opt for further refinement.
        tspRoute = threeOpt(tspRoute, optimizedDistanceMap, maxIterations)
        val currentPosition = LocationUtils.playerLocation()

        val adjustedRoute = adjustRouteForCurrentLocation(tspRoute, currentPosition)

        return adjustedRoute.map { it.position }
    }

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

    private fun improvedTSP(
        distanceMap: Map<GraphNode, Map<GraphNode, Double>>,
        maxIterations: Int,
        neighborhoodSize: Int,
    ): MutableList<GraphNode> {
        val route = greedyTSP(distanceMap).toMutableList()
        var iteration = 0
        var improved = true

        while (improved && iteration < maxIterations) {
            improved = false
            for (i in 1 until route.size - 1) {
                val jMax = (i + neighborhoodSize).coerceAtMost(route.size)
                for (j in i + 1 until jMax) {
                    val costCurrent = (distanceMap[route[i - 1]]?.get(route[i]) ?: Double.POSITIVE_INFINITY) +
                        (distanceMap[route[j - 1]]?.get(route[j]) ?: Double.POSITIVE_INFINITY)
                    val costNew = (distanceMap[route[i - 1]]?.get(route[j]) ?: Double.POSITIVE_INFINITY) +
                        (distanceMap[route[j - 1]]?.get(route[i]) ?: Double.POSITIVE_INFINITY)
                    if (costNew < costCurrent) {
                        route.subList(i, j).reverse()
                        improved = true
                        break // break inner loop on improvement
                    }
                }
                if (improved) break // restart iteration after a change
            }
            iteration++
        }
        return route
    }

    // Updated to compute both intra- and inter-cluster distances.
    private fun computeDistanceMapOptimized(targetNodes: List<GraphNode>): Map<GraphNode, Map<GraphNode, Double>> {
        val clusters = computeClusters(targetNodes)
        val result = mutableMapOf<GraphNode, MutableMap<GraphNode, Double>>()

        // Compute intra-cluster distances.
        clusters.forEach { cluster ->
            cluster.parallelStream().forEach { node ->
                val dijkstraTree = GraphUtils.findAllShortestDistances(node)
                result[node] = cluster.associateWith { target ->
                    dijkstraTree.distances[target] ?: Double.POSITIVE_INFINITY
                }.toMutableMap()
            }
        }

        // Compute inter-cluster distances.
        for (i in clusters.indices) {
            for (j in i + 1 until clusters.size) {
                val clusterA = clusters[i]
                val clusterB = clusters[j]
                for (nodeA in clusterA) {
                    val dijkstraTreeA = GraphUtils.findAllShortestDistances(nodeA)
                    for (nodeB in clusterB) {
                        val distance = dijkstraTreeA.distances[nodeB] ?: Double.POSITIVE_INFINITY
                        result[nodeA]?.put(nodeB, distance)
                    }
                }
                for (nodeB in clusterB) {
                    val dijkstraTreeB = GraphUtils.findAllShortestDistances(nodeB)
                    for (nodeA in clusterA) {
                        val distance = dijkstraTreeB.distances[nodeA] ?: Double.POSITIVE_INFINITY
                        result[nodeB]?.put(nodeA, distance)
                    }
                }
            }
        }
        return result
    }

    private fun computeClusters(targetNodes: List<GraphNode>): List<List<GraphNode>> {
        val clusters = mutableListOf<MutableList<GraphNode>>()
        val visited = mutableSetOf<GraphNode>()

        fun dfs(node: GraphNode, cluster: MutableList<GraphNode>) {
            visited.add(node)
            cluster.add(node)
            for (neighbor in node.neighbours) {
                // Ensure neighbor.key equals one of the target nodes.
                if (targetNodes.contains(neighbor.key) && !visited.contains(neighbor.key)) {
                    dfs(neighbor.key, cluster)
                }
            }
        }

        for (node in targetNodes) {
            if (!visited.contains(node)) {
                val cluster = mutableListOf<GraphNode>()
                dfs(node, cluster)
                clusters.add(cluster)
            }
        }
        return clusters
    }

    private fun greedyTSP(distanceMap: Map<GraphNode, Map<GraphNode, Double>>): List<GraphNode> {
        val startNode = distanceMap.keys.first()
        val route = mutableListOf(startNode)
        val visited = mutableSetOf(startNode)
        var current = startNode

        while (visited.size < distanceMap.size) {
            var nextNode: GraphNode? = null
            var bestDistance = Double.POSITIVE_INFINITY

            distanceMap[current]?.forEach { (candidate, distance) ->
                if (!visited.contains(candidate) && distance < bestDistance) {
                    bestDistance = distance
                    nextNode = candidate
                }
            }

            if (nextNode == null) {
                for (candidate in distanceMap.keys.filter { !visited.contains(it) }) {
                    val candidateMinDistance =
                        visited.mapNotNull { distanceMap[it]?.get(candidate) }.minOrNull() ?: Double.POSITIVE_INFINITY
                    if (candidateMinDistance < bestDistance) {
                        bestDistance = candidateMinDistance
                        nextNode = candidate
                    }
                }
            }

            nextNode?.let {
                route.add(it)
                visited.add(it)
                current = it
            } ?: break
        }
        return route
    }

    private fun adjustRouteForCurrentLocation(
        route: List<GraphNode>,
        currentLocation: LorenzVec,
    ): List<GraphNode> {
        val closestNode = route.minByOrNull { it.position.distanceSq(currentLocation) } ?: route.first()
        val idx = route.indexOf(closestNode)
        return route.drop(idx) + route.take(idx)
    }

    private fun optimizeCriticalSegments(
        route: MutableList<GraphNode>,
        distanceMap: Map<GraphNode, Map<GraphNode, Double>>,
    ) {
        if (route.size < 2) return

        val edgeCosts = mutableListOf<Pair<Int, Double>>()
        for (i in 0 until route.size - 1) {
            val cost = distanceMap[route[i]]?.get(route[i + 1]) ?: Double.POSITIVE_INFINITY
            edgeCosts.add(Pair(i, cost))
        }
        val cycleCost = distanceMap[route.last()]?.get(route.first()) ?: Double.POSITIVE_INFINITY
        edgeCosts.add(Pair(route.size - 1, cycleCost))

        val averageCost = edgeCosts.map { it.second }.average()
        val threshold = 1.2 * averageCost

        val criticalEdges = edgeCosts.filter { it.second > threshold }
        if (criticalEdges.isNotEmpty()) {
            val sortedCritical = criticalEdges.sortedByDescending { it.second }
            for ((index, _) in sortedCritical) {
                if (index < route.size - 1 && index > 0) {
                    for (j in index + 2 until route.size) {
                        if (j - index >= 2) {
                            val costBefore = (distanceMap[route[index]]?.get(route[index + 1]) ?: Double.POSITIVE_INFINITY) +
                                (distanceMap[route[j - 1]]?.get(route[j]) ?: Double.POSITIVE_INFINITY)
                            val costAfter = (distanceMap[route[index]]?.get(route[j - 1]) ?: Double.POSITIVE_INFINITY) +
                                (distanceMap[route[index + 1]]?.get(route[j]) ?: Double.POSITIVE_INFINITY)
                            if (costAfter < costBefore) {
                                route.subList(index + 1, j).reverse()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun threeOpt(
        route: MutableList<GraphNode>,
        distanceMap: Map<GraphNode, Map<GraphNode, Double>>,
        maxIterations: Int = 50,
    ): MutableList<GraphNode> {
        var improved = true
        var iteration = 0
        val n = route.size
        while (improved && iteration < maxIterations) {
            improved = false
            for (i in 0 until n - 2) {
                for (j in i + 1 until n - 1) {
                    for (k in j + 1 until n) {
                        val a = route[i]
                        val b = route[i + 1]
                        val c = route[j]
                        val d = route[j + 1]
                        val e = route[k]
                        val f = route[(k + 1) % n]

                        val costAB = distanceMap[a]?.get(b) ?: Double.POSITIVE_INFINITY
                        val costCD = distanceMap[c]?.get(d) ?: Double.POSITIVE_INFINITY
                        val costEF = distanceMap[e]?.get(f) ?: Double.POSITIVE_INFINITY
                        val currentCost = costAB + costCD + costEF

                        // One reconnection: reverse segments (i+1..j) and (j+1..k)
                        val costAC = distanceMap[a]?.get(c) ?: Double.POSITIVE_INFINITY
                        val costBE = distanceMap[b]?.get(e) ?: Double.POSITIVE_INFINITY
                        val costDF = distanceMap[d]?.get(f) ?: Double.POSITIVE_INFINITY
                        val newCost = costAC + costBE + costDF

                        if (newCost < currentCost) {
                            route.subList(i + 1, j + 1).reverse()
                            route.subList(j + 1, k + 1).reverse()
                            improved = true
                        }
                    }
                }
            }
            iteration++
        }
        return route
    }
}
