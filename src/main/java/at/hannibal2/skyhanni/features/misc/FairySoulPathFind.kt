package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.util.BlockPos
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

object FairySoulPathFind {
    val config get() = SkyHanniMod.feature.misc

    private var lastRender = SimpleTimeMark.farPast()

    @JvmStatic
    fun render() {
        lastRender = SimpleTimeMark.now()
    }

    @JvmStatic
    fun updateList(list: MutableList<BlockPos>, found: Int, total: Int) {
        val graph = IslandGraphs.currentIslandGraph ?: return
        if (lastRender.passedSince() > 300.milliseconds) return
        if (!config.neuSoulsPathFind) return

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
            val (path, distance) = GraphUtils.findShortestPathAsGraphWithDistance(playerNode, node)
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
//         exactPath()
    }

    // Step 1: Preload the 50×50 Distance Matrix
    fun computeDistanceMap(targetNodes: List<GraphNode>): Map<GraphNode, Map<GraphNode, Double>> {
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

    // Step 2: Fast Greedy TSP Algorithm (~1ms for 50 nodes)
    fun greedyTSP(distanceMap: Map<GraphNode, Map<GraphNode, Double>>): List<GraphNode> {
        // Pick the first node as the start (or choose any other)
        val startNode = distanceMap.keys.first()
        val route = mutableListOf(startNode)
        val visited = mutableSetOf(startNode)
        var current = startNode

        while (visited.size < distanceMap.size) {
            var nextNode: GraphNode? = null
            var bestDistance = Double.POSITIVE_INFINITY

            // Look for the nearest unvisited neighbor from 'current'
            distanceMap[current]?.forEach { (candidate, distance) ->
                if (candidate !in visited && distance < bestDistance) {
                    bestDistance = distance
                    nextNode = candidate
                }
            }

            nextNode?.let {
                route.add(it)
                visited.add(it)
                current = it
            } ?: break  // if no next node found (shouldn't happen in a connected graph)
        }

        // Optionally, complete the cycle by returning to the start:
        // route.add(startNode)

        return route
    }


//     // Usage Example:
//     fun main() {
//         // Assume `tspRoute` is computed already (a List<GraphNode> forming a TSP cycle).
//         // And you have your player's current position:
//         val currentPosition: LorenzVec = LocationUtils.playerLocation() // your function here
//
//         // Adjust the route so that it starts with the node closest to currentPosition.
//         val adjustedRoute = adjustRouteForCurrentLocation(tspRoute, currentPosition)
//
//         // Optionally, get the path from your current location to the start of the TSP route.
//         val pathToStart = getPathFromCurrentLocation(currentPosition, adjustedRoute.first())
//
//         // Now, you can use pathToStart followed by adjustedRoute as your complete path.
//     }

    // Example main function that ties everything together.
    fun testCoolNewPath() {
        val allNodes = IslandGraphs.currentIslandGraph ?: return

        // 1. Retrieve target nodes.
        var targetNodes: List<GraphNode>
        val targetNodesTime = measureTimeMillis {
            targetNodes = getTargetNodes(allNodes)
        }
        println("getTargetNodes took $targetNodesTime ms.")

        // 2. Precompute the 50x50 distance map.
        var distanceMap: Map<GraphNode, Map<GraphNode, Double>>
        val distanceMapTime = measureTimeMillis {
            distanceMap = computeDistanceMap(targetNodes)
        }
        println("computeDistanceMap took $distanceMapTime ms.")

        // 3. Run the Greedy TSP algorithm.
        var tspRoute: List<GraphNode>
        val tspRouteTime = measureTimeMillis {
            tspRoute = greedyTSP(distanceMap)
        }
        println("greedyTSP took $tspRouteTime ms.")

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

        // 6. Compute a path from the player's current location to the first node of the adjusted route.
        var pathToStart: List<LorenzVec>
        val pathToStartTime = measureTimeMillis {
            pathToStart = getPathFromCurrentLocation(currentPosition, adjustedRoute.first())
        }
        println("getPathFromCurrentLocation took $pathToStartTime ms.")

//         SkyHanniMod.launchCoroutine {
//             for (node in adjustedRoute) {
//                 val pos = node.position
//                 IslandGraphs.renderPath(nodes = adjustedRoute)
//
//             }
//         }

        // 7. Render the final path.
        val renderPathTime = measureTimeMillis {
            IslandGraphs.renderPath(nodes = adjustedRoute)
        }
        println("renderPath took $renderPathTime ms.")
    }

    // Dummy helper – replace with your own method to obtain target nodes.
    fun getTargetNodes(allNodes: Graph): List<GraphNode> {

        return allNodes.filter { GraphNodeTag.NPC in it.tags }
        // For example, filter nodes from your island graph:
        // return IslandGraphs.currentIslandGraph?.nodes?.filter { it.tagNames.contains("target") }?.take(50) ?: emptyList()
//         return listOf() // Replace with your actual node list.
    }

    // Given: a TSP route (a list of target GraphNodes forming a cycle)
// and a current location (as a LorenzVec), plus a helper to compute distance.
    fun adjustRouteForCurrentLocation(
        route: List<GraphNode>,
        currentLocation: LorenzVec
    ): List<GraphNode> {
        // Find the closest node in the route by comparing the squared distances.
        val closestNode = route.minByOrNull { it.position.distanceSq(currentLocation) } ?: route.first()
        // Rotate the route so that the closest node comes first.
        val idx = route.indexOf(closestNode)
        return route.drop(idx) + route.take(idx)
    }

    // If you need a path from your actual location to the chosen start node,
// you can use your GraphUtils to compute the shortest path (if currentLocation is
// associated with a node on the graph, or by mapping your current location to the nearest node):
    fun getPathFromCurrentLocation(
        currentLocation: LorenzVec,
        startNode: GraphNode
    ): List<LorenzVec> {
        // Assume nearestNodeOnCurrentIsland(currentLocation) gives you the closest GraphNode to your location.
        val currentNode = GraphUtils.nearestNodeOnCurrentIsland(currentLocation)
        return GraphUtils.findShortestPath(currentNode, startNode)
    }
}
