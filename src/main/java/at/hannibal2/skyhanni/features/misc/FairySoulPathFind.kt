package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.util.BlockPos
import java.util.TreeMap
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

object FairySoulPathFind {
    val config get() = SkyHanniMod.feature.misc

    private var lastRender = SimpleTimeMark.farPast()

    @JvmStatic
    fun render() {
        lastRender = SimpleTimeMark.now()
    }

    var missing = setOf<LorenzVec>()
    var lastMissing: Int? = null

    @JvmStatic
    fun updateList(
        list: MutableList<BlockPos>,
        found: Int,
        total: Int,
        missingSoulsDistanceSqMap: TreeMap<Double, BlockPos>,
    ) {
        missing = missingSoulsDistanceSqMap.values.map { it.toLorenzVec() }.toSet()
        val graph = IslandGraphs.currentIslandGraph ?: return
        if (lastRender.passedSince() > 300.milliseconds) return
        if (!config.neuSoulsPathFind) return
        if (config.neuSoulsPathFindBetter) {
            tryRunBetter()
            return
        }

        val souls = mutableMapOf<LorenzVec, GraphNode>()

        for (pos in list) {
            val vec = pos.toLorenzVec()
            val node = graph.minBy { it.position.distance(vec) }
            souls[vec] = node
        }

        val playerNode = graph.minBy { it.position.distanceSqToPlayer() }

        val distances = mutableMapOf<LorenzVec, Double>()
        for ((location, node) in souls) {
            val lastDistance = node.position.distance(location)
            val (_, distance) = GraphUtils.findShortestPathAsGraphWithDistance(playerNode, node)
            distances[location] = distance + lastDistance
        }

        val percentage = (found.toDouble() / total) * 100
        val label = "§b$found/$total (${percentage.roundTo(1)}%)"

        val closest = distances.minBy { it.value }.key
        IslandGraphs.pathFind(
            closest,
            "§5NEU Souls $label",
            LorenzColor.DARK_PURPLE.toColor(),
            condition = { config.neuSoulsPathFind && lastRender.passedSince() < 300.milliseconds },
        )
    }

    private fun tryRunBetter() {
        if (lastMissing != missing.size) {
            lastMissing = missing.size
            testCoolNewPath()
        }
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
                    val costCurrent = distanceMap[route[i - 1]]!![route[i]]!! + distanceMap[route[j - 1]]!![route[j]]!!
                    val costNew = distanceMap[route[i - 1]]!![route[j]]!! + distanceMap[route[j - 1]]!![route[i]]!!
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

    // TODO cache
    private fun testCoolNewPath() {
        val allNodes = IslandGraphs.currentIslandGraph ?: return

        // 1. Retrieve target nodes.
        var targetNodes: List<GraphNode>
        val targetNodesTime = measureTimeMillis {
            targetNodes = getTargetNodes(allNodes)
        }
        println("getTargetNodes took $targetNodesTime ms.")

        if (targetNodes.isEmpty()) {
            ChatUtils.chat("is empty")
            return
        }

        // 2. Precompute the 50x50 distance map.
        var distanceMap: Map<GraphNode, Map<GraphNode, Double>>
        val distanceMapTime = measureTimeMillis {
            distanceMap = computeDistanceMap(targetNodes)
        }
        println("computeDistanceMap took $distanceMapTime ms.")

        // 3. Run the Greedy TSP algorithm.
        var tspRoute: List<GraphNode>
        val tspRouteTime = measureTimeMillis {
            tspRoute = improvedTSP(distanceMap)
        }
        println("improvedTSP took $tspRouteTime ms.")

        // 4. Retrieve the player's current location.
        var currentPosition: LorenzVec
        val currentPositionTime = measureTimeMillis {
            currentPosition = LocationUtils.playerLocation()
        }
        println("LocationUtils.playerLocation took $currentPositionTime ms.")

        // 5. Adjust the route so that it starts with the node closest to the current position.
        var adjustedRoute: List<GraphNode>
        val adjustRouteTime = measureTimeMillis {
            adjustedRoute = adjustRouteForCurrentLocation(tspRoute, currentPosition)
        }
        println("adjustRouteForCurrentLocation took $adjustRouteTime ms.")

        pathTo(adjustedRoute.map { it.position }, 0)
    }

    private fun pathTo(adjustedRoute: List<LorenzVec>, index: Int) {
        val totalSize = adjustedRoute.size
        if (index == totalSize) {
            ChatUtils.chat("done")
            return
        }
        val lorenzVec = adjustedRoute[index]
        // TODO only start path once the fairy soul is clicked
        IslandGraphs.pathFind(
            lorenzVec,
            "${index + 1}/$totalSize",
            onFound = {
                pathTo(adjustedRoute, index + 1)
            },
            condition = { true },
        )
    }

    // TODO write villager hub feature later, fix duplicate andrew
//     val hubVillagers = setOf(
//         "Andrew", "Duke", "Felix", "Jack", "Jamie", "Leo",
//         "Liam", "Lynn", "Ryu", "Stella", "Tom", "Vex",
//     )

    private fun getTargetNodes(allNodes: Graph): List<GraphNode> {
        return missing.mapNotNull { pos ->
            allNodes.minByOrNull { it.position.distance(pos) }
        }

//         return allNodes.filter { GraphNodeTag.NPC in it.tags && it.name in hubVillagers }
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
