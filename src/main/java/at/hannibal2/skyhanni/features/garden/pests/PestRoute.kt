package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestEntityResolver.LoadedPest
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.BlockUtils.raycast
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasEtherwarp
import at.hannibal2.skyhanni.utils.compat.EntityCompat.deceased
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToCrosshair
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.world.phys.HitResult
import kotlin.math.floor

@SkyHanniModule
object PestRoute {

    private data class EtherwarpTarget(val block: LorenzVec, val nearbyPests: Int)

    private val config get() = PestApi.config
    private val routeColor = LorenzColor.RED.toColor()
    private var route = emptyList<LoadedPest>()
    private var etherwarpTarget: EtherwarpTarget? = null

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        val enabled = isEnabled()
        if (!enabled) {
            route = emptyList()
            etherwarpTarget = null
            return
        }
        if (!event.isMod(ROUTE_UPDATE_INTERVAL_TICKS)) return

        val pests = PestEntityResolver.getLoadedPests()
            .filter { it.isVisiblePest() }

        route = calculateRoute(LocationUtils.playerLocation(), pests)
        etherwarpTarget = when {
            !isEtherwarpTargetEnabled() -> null
            event.isMod(ETHERWARP_UPDATE_INTERVAL_TICKS) -> findEtherwarpTarget(pests)
            else -> etherwarpTarget
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled() || route.isEmpty()) return

        val visibleRoute = route.filter { it.isVisiblePest() }
        if (visibleRoute.isEmpty()) return

        if (config.shortestPestRoute) {
            event.drawLineToCrosshair(visibleRoute.first().location, routeColor, lineWidth = 3, depth = true)

            visibleRoute.zipWithNext().forEach { (previous, next) ->
                event.draw3DLine(previous.location, next.location, routeColor, lineWidth = 3, depth = true)
            }

            var previousLocation = LocationUtils.playerLocation()
            visibleRoute.forEachIndexed { index, pest ->
                val location = pest.location
                val rightClicks = (previousLocation.distance(location) / RIGHT_CLICK_TELEPORT_DISTANCE).toInt()
                event.drawDynamicText(
                    previousLocation.middle(location).add(y = 0.5),
                    "§e$rightClicks RC",
                    scaleMultiplier = 1.2,
                    seeThroughBlocks = false,
                )
                event.drawDynamicText(
                    location.add(y = 0.75),
                    "§c§l${index + 1} §7${pest.type.displayName}",
                    scaleMultiplier = 1.8,
                    seeThroughBlocks = false,
                )
                previousLocation = location
            }
        }

        if (isEtherwarpTargetEnabled()) {
            etherwarpTarget?.let { target ->
                val landingPosition = target.block.add(0.5, 1.0, 0.5)
                event.drawWaypointFilled(target.block, LorenzColor.AQUA.toColor(), seeThroughBlocks = false)
                event.drawLineToCrosshair(
                    target.block.add(0.5, 0.5, 0.5),
                    LorenzColor.AQUA.toColor(),
                    lineWidth = 2,
                    depth = true,
                )
                event.drawDynamicText(
                    landingPosition.add(y = 1.0),
                    "§bAim Etherwarp Here §7(${target.nearbyPests} pests)",
                    scaleMultiplier = 1.5,
                    seeThroughBlocks = false,
                )
            }
        }
    }

    private fun isEnabled() = GardenApi.inGarden() && (config.shortestPestRoute || isEtherwarpTargetEnabled())

    private fun isEtherwarpTargetEnabled() =
        config.etherwarpPestTarget && InventoryUtils.getItemInHand()?.hasEtherwarp() == true

    private fun LoadedPest.isVisiblePest() =
        !entity.deceased && canBeSeen(viewDistance = PEST_VIEW_DISTANCE)

    private fun calculateRoute(start: LorenzVec, pests: List<LoadedPest>): List<LoadedPest> {
        if (pests.size <= 1) return pests

        val order = if (pests.size <= MAX_EXACT_PESTS) {
            shortestOpenRoute(start, pests.map { it.location })
        } else {
            nearestNeighbourRoute(start, pests.map { it.location })
        }
        return order.map(pests::get)
    }

    private fun findEtherwarpTarget(pests: List<LoadedPest>): EtherwarpTarget? {
        if (pests.isEmpty()) return null

        val playerEye = LocationUtils.playerEyeLocation()
        val pestLocations = pests.map { it.location }
        return buildSet {
            for (pestLocation in pestLocations) {
                val baseX = floor(pestLocation.x).toInt()
                val baseY = floor(pestLocation.y).toInt()
                val baseZ = floor(pestLocation.z).toInt()
                for (xOffset in -TARGET_SEARCH_RADIUS..TARGET_SEARCH_RADIUS) {
                    for (zOffset in -TARGET_SEARCH_RADIUS..TARGET_SEARCH_RADIUS) {
                        for (yOffset in -TARGET_VERTICAL_SEARCH..TARGET_VERTICAL_SEARCH) {
                            add(LorenzVec(baseX + xOffset, baseY + yOffset, baseZ + zOffset))
                        }
                    }
                }
            }
        }.asSequence()
            .filter { it.isValidEtherwarpTarget(playerEye) }
            .map { block ->
                val landingPosition = block.add(0.5, 1.0, 0.5)
                val nearbyPests = pestLocations.count { it.distance(landingPosition) <= PEST_CLUSTER_RADIUS }
                val totalPestDistance = pestLocations.sumOf { it.distance(landingPosition) }
                Triple(block, nearbyPests, totalPestDistance)
            }
            .maxWithOrNull(
                compareBy<Triple<LorenzVec, Int, Double>> { it.second }
                    .thenBy { -it.third },
            )
            ?.let { (block, nearbyPests) -> EtherwarpTarget(block, nearbyPests) }
    }

    private fun LorenzVec.isValidEtherwarpTarget(playerEye: LorenzVec): Boolean {
        val targetCenter = add(0.5, 0.5, 0.5)
        if (playerEye.distance(targetCenter) !in MIN_ETHERWARP_DISTANCE..MAX_ETHERWARP_DISTANCE) return false

        val state = getBlockStateAt()
        if (state.isAir || !state.blocksMotion()) return false
        if (!add(y = 1).getBlockStateAt().isAir || !add(y = 2).getBlockStateAt().isAir) return false

        val hit = raycast(playerEye, targetCenter)
        return hit.type == HitResult.Type.BLOCK && hit.blockPos == toBlockPos()
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

    private const val ROUTE_UPDATE_INTERVAL_TICKS = 10
    private const val ETHERWARP_UPDATE_INTERVAL_TICKS = 20
    private const val MAX_EXACT_PESTS = 10
    private const val PEST_VIEW_DISTANCE = 400
    private const val RIGHT_CLICK_TELEPORT_DISTANCE = 12.0
    private const val MIN_ETHERWARP_DISTANCE = 8.0
    private const val MAX_ETHERWARP_DISTANCE = 61.0
    private const val TARGET_SEARCH_RADIUS = 4
    private const val TARGET_VERTICAL_SEARCH = 3
    private const val PEST_CLUSTER_RADIUS = 18.0
}
