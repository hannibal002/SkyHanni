package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.util.BlockPos
import java.util.TreeMap
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object NeuSoulPathFind {
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
    private var goodRoute = emptyList<LorenzVec>()
    private var currentIndex = 0

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        missing = emptySet()
        lastMissing = 0
        found = 0
        total = 0

        goodRoute = emptyList()
        currentIndex = 0
    }

    private var lastFastReminder = SimpleTimeMark.farPast()

    @JvmStatic
    fun updateList(allSouls: List<BlockPos>, missingSouls: TreeMap<Double, BlockPos>) {
        val graph = IslandGraphs.currentIslandGraph ?: return
        if (lastRender.passedSince() > 300.milliseconds) return
        if (!config.neuSoulsPathFind) return
        // we dont need neu souls if fast souls is enabled
        if (config.fastFairySouls) return

        remindFast()

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

    private fun remindFast() {
        if (lastFastReminder.passedSince() > 1.hours) {
            lastFastReminder = SimpleTimeMark.now()
            ChatUtils.clickableChat(
                "SkyHanni has an even faster Fairy Soul pathfinding logic. Click here to enable it.",
                onClick = {
                    config.neuSoulsPathFind = false
                    config.fastFairySouls = true
                },
            )
        }
    }

    // TODO write villager hub feature later, fix duplicate andrew
//     val hubVillagers = setOf(
//         "Andrew", "Duke", "Felix", "Jack", "Jamie", "Leo",
//         "Liam", "Lynn", "Ryu", "Stella", "Tom", "Vex",
//     )

//         return allNodes.filter { GraphNodeTag.NPC in it.tags && it.name in hubVillagers }
}
