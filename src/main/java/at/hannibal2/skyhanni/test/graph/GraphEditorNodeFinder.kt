package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.GraphUtils.getNearestToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import kotlin.collections.orEmpty

@SkyHanniModule
object GraphEditorNodeFinder {

    private val nodesAlreadyFound = mutableListOf<LorenzVec>()
    private val nodesToFind: List<LorenzVec>
        get() = IslandGraphs.currentIslandGraph?.map { it.position }?.filter { it !in nodesAlreadyFound }.orEmpty()
    private var currentNodeToFind: LorenzVec? = null
    var active = false

    fun handleAllNodeFind() {
        if (!active) return

        if (nodesToFind.isEmpty()) return
        val closest = nodesToFind.getNearestToPlayer()
        if (distanceSqToPlayer(closest) >= 9) return
        nodesAlreadyFound.add(closest)

        if (nodesToFind.isEmpty()) {
            currentNodeToFind = null
            ChatUtils.chat("Found all nodes on this island")
            TitleManager.sendTitle("§eAll Found!")
            active = false
            return
        }

        calculateNewAllNodeFind()
    }

    fun calculateNewAllNodeFind(): LorenzVec {
        val next = GraphUtils.findShortestDistancesOnCurrentIsland(nodesToFind).lastVisitedNode.position

        val max = IslandGraphs.currentIslandGraph?.size ?: -1
        val todo = nodesToFind.size
        val done = max - todo
        val percentage = (done.toDouble() / max.toDouble()) * 100
        val node = GraphUtils.nearestNodeOnCurrentIsland(next)
        node.pathFind(
            "Progress: ${done.addSeparators()}/${max.addSeparators()} (${percentage.roundTo(2)}%)",
            condition = { active },
        )
        currentNodeToFind = next
        return next
    }

    fun toggleFindAll() {
        active = !active
        if (active) {
            nodesAlreadyFound.clear()
            calculateNewAllNodeFind()
            ChatUtils.chat("Graph navigation over all nodes started.")
        } else {
            ChatUtils.chat("Graph navigation over all nodes stopped.")
        }
    }
}
