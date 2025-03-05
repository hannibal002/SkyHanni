package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.util.BlockPos
import java.util.TreeMap
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object FairySoulPathFind {
    val config get() = SkyHanniMod.feature.misc

    private var lastRender = SimpleTimeMark.farPast()

    @JvmStatic
    fun render() {
        lastRender = SimpleTimeMark.now()
    }

    private var missing = setOf<LorenzVec>()
    private var lastMissing: Int? = null
    private var found = 0
    private var total = 0
    private var foundButNotClickedSoul: LorenzVec? = null

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        foundButNotClickedSoul?.let {
            if (it.distanceToPlayer() > 5) {
                pathTo(it)
                foundButNotClickedSoul = null
            }
        }
    }

    @JvmStatic
    fun updateList(allSouls: List<BlockPos>, missingSouls: TreeMap<Double, BlockPos>) {
        val graph = IslandGraphs.currentIslandGraph ?: return
        if (lastRender.passedSince() > 300.milliseconds) return
        if (!config.neuSoulsPathFind) return

        val missingLocally = mutableMapOf<LorenzVec, GraphNode>()
        var foundLocally = 0
        for (pos in allSouls) {
            val vec = pos.toLorenzVec()
            val node = graph.minBy { it.position.distance(vec) }
            val distance = node.position.distance(vec)
            // we skip some souls that are too far away from the closest node, especially for dwarven mines/glacite tunnels
            if (distance < 15) {
                if (pos in missingSouls.values) {
                    missingLocally[vec] = node
                } else {
                    foundLocally++
                }
            }
        }
        missing = missingLocally.keys.toSet()
        found = foundLocally
        total = missing.size + found

        if (config.neuSoulsPathFindBetter) {
            tryRunBetter()
            return
        }

        // stopped bc we are done already
        if (missing.isEmpty()) return

        val playerNode = graph.minBy { it.position.distanceSqToPlayer() }

        val distances = mutableMapOf<LorenzVec, Double>()
        for ((location, node) in missingLocally) {
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
//         foundButNotClickedSoul?.let {
//             if (found == total) {
//                 IslandGraphs.pathFind(
//                     LocationUtils.playerLocation(),
//                     "§5NEU Souls $found/$total",
//                     condition = { true },
//                 )
//             }
//         }
        foundButNotClickedSoul = null
        val allNodes = IslandGraphs.currentIslandGraph ?: return

        val current = LorenzUtils.skyBlockIsland
        // 1. Retrieve target nodes.
        var targetNodes: List<GraphNode>
        val targetNodesTime = measureTimeMillis {
            targetNodes = getTargetNodes(allNodes)
        }
        ChatUtils.chat("targetNodes in $current: ${targetNodes.size}")
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
        ChatUtils.chat("adjustedRoute in $current: ${adjustedRoute.size}")

        pathTo(adjustedRoute.map { it.position }.first())
    }

    private fun pathTo(loc: LorenzVec) {
        ChatUtils.chat("pathTo")
        IslandGraphs.pathFind(
            loc,
            "§5NEU Souls ${found + 1}/$total",
            LorenzColor.DARK_PURPLE.toColor(),
            onFound = {
                foundButNotClickedSoul = loc
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
