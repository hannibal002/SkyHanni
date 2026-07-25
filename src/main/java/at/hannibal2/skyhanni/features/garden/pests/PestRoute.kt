package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation

@SkyHanniModule
object PestRoute {

    private val config get() = PestApi.config
    private val routeColor = LorenzColor.RED.toColor()
    private var route = emptyList<Mob>()

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        if (!isEnabled()) {
            route = emptyList()
            return
        }

        val pests = MobData.entityToMob.values
            .filter { it.isAlive && PestType.getByNameOrNull(it.name) != null }
            .distinct()

        route = calculateRoute(LocationUtils.playerLocation(), pests)
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled() || route.isEmpty()) return

        var previous = event.exactPlayerEyeLocation()
        route.forEachIndexed { index, pest ->
            if (!pest.isAlive) return@forEachIndexed

            val location = pest.centerCords
            event.draw3DLine(previous, location, routeColor, lineWidth = 3, depth = false)
            event.drawDynamicText(
                location.add(y = 1.5),
                "§c§l${index + 1} §7${pest.name}",
                scaleMultiplier = 1.2,
            )
            previous = location
        }
    }

    private fun isEnabled() = GardenApi.inGarden() && config.shortestPestRoute

    private fun calculateRoute(start: LorenzVec, pests: List<Mob>): List<Mob> {
        if (pests.size <= 1) return pests

        val order = if (pests.size <= MAX_EXACT_PESTS) {
            shortestOpenRoute(start, pests.map { it.centerCords })
        } else {
            nearestNeighbourRoute(start, pests.map { it.centerCords })
        }
        return order.map(pests::get)
    }

    /**
     * Held-Karp dynamic programming for an open route with a fixed start.
     * Unlike the classic travelling salesman cycle, this does not return to the player.
     */
    internal fun shortestOpenRoute(start: LorenzVec, points: List<LorenzVec>): List<Int> {
        val count = points.size
        if (count <= 1) return points.indices.toList()

        val stateCount = 1 shl count
        val costs = Array(stateCount) { DoubleArray(count) { Double.POSITIVE_INFINITY } }
        val previous = Array(stateCount) { IntArray(count) { -1 } }

        for (end in points.indices) {
            costs[1 shl end][end] = start.distance(points[end])
        }

        for (mask in 1 until stateCount) {
            for (end in points.indices) {
                val endBit = 1 shl end
                if (mask and endBit == 0) continue
                val previousMask = mask xor endBit
                if (previousMask == 0) continue

                for (previousEnd in points.indices) {
                    if (previousMask and (1 shl previousEnd) == 0) continue
                    val candidate = costs[previousMask][previousEnd] + points[previousEnd].distance(points[end])
                    if (candidate < costs[mask][end]) {
                        costs[mask][end] = candidate
                        previous[mask][end] = previousEnd
                    }
                }
            }
        }

        var mask = stateCount - 1
        var end = points.indices.minBy { costs[mask][it] }
        val reversedRoute = ArrayList<Int>(count)
        while (end != -1) {
            reversedRoute.add(end)
            val nextEnd = previous[mask][end]
            mask = mask xor (1 shl end)
            end = nextEnd
        }
        return reversedRoute.asReversed()
    }

    private fun nearestNeighbourRoute(start: LorenzVec, points: List<LorenzVec>): List<Int> {
        val remaining = points.indices.toMutableSet()
        val result = ArrayList<Int>(points.size)
        var current = start

        while (remaining.isNotEmpty()) {
            val next = remaining.minBy { current.distance(points[it]) }
            result.add(next)
            remaining.remove(next)
            current = points[next]
        }
        return result
    }

    private const val MAX_EXACT_PESTS = 15
}
