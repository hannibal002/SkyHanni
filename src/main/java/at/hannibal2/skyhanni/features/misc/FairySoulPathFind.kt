package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
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
    private var lastMissing = 0
    private var found = 0
    private var total = 0
    private var foundButNotClickedSoul: LorenzVec? = null
    private var goodRoute = emptyList<LorenzVec>()
    private var currentIndex = 0

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        missing = emptySet()
        lastMissing = 0
        found = 0
        total = 0

        foundButNotClickedSoul = null
        goodRoute = emptyList()
        currentIndex = 0
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        val lastSoul = foundButNotClickedSoul ?: return

        // disabled or last soul found
        if (lastRender.passedSince() > 300.milliseconds) {
            foundButNotClickedSoul = null
        }

        if (lastSoul.distanceToPlayer() > 5) {
            pathTo(lastSoul)
            foundButNotClickedSoul = null
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
        if (missing.size != missingLocally.keys.toSet().size) {
            ChatUtils.chat("NEU update: missing changed ${missing.size} -> ${missingLocally.keys.toSet().size}")
        }
        missing = missingLocally.keys.toSet()
        if (found != foundLocally) {
            ChatUtils.chat("NEU update: found changed $found -> $foundLocally")
        }
        found = foundLocally
        if (total != missing.size + found) {
            ChatUtils.chat("NEU update: total changed $total -> ${missing.size + found}")
        }
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
        if (missing.size > lastMissing) {
            // when new souls are enabled, e.g. the user runs /neusouls unclear, we want to recalculate
            testCoolNewPath(forceUpdate = true)
        } else if (lastMissing > missing.size) {
            testCoolNewPath(forceUpdate = false)
        }
        lastMissing = missing.size
    }

    fun testCoolNewPath(forceUpdate: Boolean) {
        foundButNotClickedSoul = null
        if (goodRoute.isNotEmpty() && !forceUpdate) {
            currentIndex++
            pathTo(goodRoute[currentIndex])
            return
        }
        val allNodes = IslandGraphs.currentIslandGraph ?: return

        var targetNodes: List<GraphNode>
        val targetNodesTime = measureTimeMillis {
            targetNodes = getTargetNodes(allNodes)
        }
        println("getTargetNodes took $targetNodesTime ms.")

        if (targetNodes.isEmpty()) {
            ChatUtils.chat("is empty")
            return
        }

        val maxIterations = SkyHanniDebugsAndTests.a.toInt()
        val neighborhoodSize = SkyHanniDebugsAndTests.b.toInt()

        val routeTime = measureTimeMillis {
            goodRoute = NavigationUtils.getRoute(targetNodes, maxIterations, neighborhoodSize)
        }
        val length = GraphUtils.calculatePathLength(goodRoute)

        val ms = routeTime.milliseconds
        val distance = length.roundTo(0).addSeparators()
        ChatUtils.chat("route ${targetNodes.size}n ($maxIterations/$neighborhoodSize) took $ms/${distance}m")

        currentIndex = 0
        if (found == 0 && total != goodRoute.size) {
            ErrorManager.skyHanniError(
                "Advanced route planning could not find one path between all goals",
                "total" to total,
                "goodRoute size" to goodRoute.size,
                "island" to LorenzUtils.skyBlockIsland,
            )
        }
        pathTo(goodRoute.first())
    }

    private fun pathTo(loc: LorenzVec) {
        if (found == total) return
        IslandGraphs.pathFind(
            loc,
            "§5NEU Souls $found/$total",
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
}
