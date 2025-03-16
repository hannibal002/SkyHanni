package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.util.BlockPos
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

        var skipped = 0
        for (pos in list) {
            val vec = pos.toLorenzVec()
            val node = graph.minBy { it.position.distance(vec) }
            val distance = node.position.distance(vec)
            if (distance > 15) {
                // we skip some souls that are too far away from the closest node, especially for dwarven mines/glacite tunnels
                skipped++
            } else {
                souls[vec] = node
            }
        }
        // stopped bc we are done already
        if (souls.isEmpty()) return

        val playerNode = graph.minBy { it.position.distanceSqToPlayer() }

        val distances = mutableMapOf<LorenzVec, Double>()
        for ((location, node) in souls) {
            val lastDistance = node.position.distance(location)
            val (path, distance) = GraphUtils.findShortestPathAsGraphWithDistance(playerNode, node)
            distances[location] = distance + lastDistance
        }

        val displayTotal = total - skipped

        val percentage = (found.toDouble() / displayTotal) * 100
        val label = "§b$found/$displayTotal (${percentage.roundTo(1)}%)"

        val closest = distances.minBy { it.value }.key
        IslandGraphs.pathFind(
            closest,
            "§5NEU Souls $label",
            LorenzColor.DARK_PURPLE.toColor(),
            condition = { config.neuSoulsPathFind && lastRender.passedSince() < 300.milliseconds },
        )
    }
}
