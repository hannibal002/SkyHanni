package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.diana.BurrowDugEvent
import at.hannibal2.skyhanni.events.diana.BurrowGuessEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RaycastUtils
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import net.minecraft.core.particles.ParticleTypes
import kotlin.math.abs
import kotlin.math.sign
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ArrowGuessBurrow {
    private val config get() = SkyHanniMod.feature.event.diana

    private const val SHAFT_LENGTH = 20
    private const val PARTICLE_DETECTION_TOLERANCE = 0.12
    private const val COUNT_NEAR_TIP = 4
    private const val COUNT_NEAR_BASE = 2
    private const val EPSILON = 1e-6

    private val points: MutableSet<LorenzVec> = mutableSetOf()
    private val recentArrowParticles = TimeLimitedSet<LorenzVec>(1.minutes)

    private var newArrow = true
    private var failures = 0

    @HandleEvent(onlyOnIsland = IslandType.HUB, receiveCancelled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!newArrow) return

        if (event.distanceToPlayer > 6) return
        if (event.type != ParticleTypes.DUST) return
        if (event.count != 0) return
        if (event.speed != 1.0f) return

        // offset is color for some reason
        val range = getArrowRange(event.offset) ?: return

        if (!recentArrowParticles.add(event.location)) return
        points.add(event.location)

        val arrow = detectArrow(points) ?: return
        GriffinBurrowHelper.removeGuess(arrow.origin)
        newArrow = false
        points.clear()
        findClosestValidBlockToRayNew(arrow, range) ?: run {
            failures++
            if (config.warnOnFail) {
                TitleManager.sendTitle("§eUse Spade", duration = 3.seconds)
            }
            return
        }
    }

    @HandleEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        if (event.current != event.max) {
            points.clear()
            newArrow = true
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Arrow Burrow Guess")

        if (!DianaApi.isDoingDiana()) {
            event.addIrrelevant("not doing diana")
            return
        }

        event.addData {
            add("failures: $failures")
        }
    }

    fun getArrowRange(offset: LorenzVec): IntRange? {
        return when (offset) {
            LorenzVec(0, 128, 0) -> IntRange(0, 117) // yellow
            LorenzVec(255, 255, 0) -> IntRange(112, 282) // red
            LorenzVec(255, 0, 0) -> IntRange(281, 600) // black
            else -> null
        }
    }

    private fun findClosestValidBlockToRayNew(ray: RaycastUtils.Ray, range: IntRange): LorenzVec? {
        val bounds = IslandType.HUB.islandData?.boundingBox ?: return null
        if (!bounds.isInside(ray.origin)) return null // guarantees exit point is first intersect
        // you technically don't need to find the endpoint for this, but it makes it simpler so why not
        val endPoint = RaycastUtils.intersectAABBWithRay(bounds, ray)?.second ?: return null

        val diff = endPoint.minus(ray.origin).toDoubleArray()
        val axisIndex = diff.withIndex()
            .filter { (_, value) -> abs(value) > 0.9 } // only if the axis isn't the same block
            .minByOrNull { (_, value) -> abs(value) } // find the axis with the least change
            ?.index
            ?: return null

        val candidates = mutableMapOf<LorenzVec, Pair<Double, Double>>() // position mapped to scaledDistToRay and distFromOrigin
        val endPointArray = endPoint.toDoubleArray()
        val originArray = ray.origin.toDoubleArray()
        val directionArray = ray.direction.toDoubleArray()

        val iterations = abs(endPointArray[axisIndex] - originArray[axisIndex])
        for (i in 1..iterations.toInt()) {
            val axisValue = originArray[axisIndex] + i * sign(directionArray[axisIndex])
            val candidatePoint = RaycastUtils.findPointOnRay(ray, axisIndex, axisValue) ?: continue
            val candidateBlock = candidatePoint.roundToBlock()
            if (!GriffinBurrowHelper.isBlockValid(candidateBlock)) continue
            val blockCenter = candidateBlock.add(0.5, 0.5, 0.5)
            val distanceToRay = RaycastUtils.findDistanceToRay(ray, blockCenter)

            val distanceFromOrigin = candidatePoint.distance(ray.origin)

            // take the ratio to account for errors
            val scaledDistance = (distanceToRay * 500000 / distanceFromOrigin)

            candidates[candidateBlock] = Pair(scaledDistance.roundTo(2), distanceFromOrigin)
        }

        if (candidates.isEmpty()) return null
        val minValue = candidates.values.minOf { it.first }
        val possibilities = candidates.filterValues { it.first == minValue }
        val withinRange = possibilities.filterValues { it.second.toInt() in range }.map { it.key }
        if (withinRange.isEmpty()) return null

        BurrowGuessEvent(GriffinBurrowHelper.GuessEntry(withinRange)).post()

        return withinRange[0]
    }

    private fun detectArrow(points: MutableSet<LorenzVec>): RaycastUtils.Ray? {
        val line = findLine(points, SHAFT_LENGTH, PARTICLE_DETECTION_TOLERANCE)
        if (line.isEmpty()) return null

        // the head of the arrow intersects with the particle one off the end
        // findLine only returns a full line with 20 points or an emptyList so this is safe
        val candidate1 = line[1]
        val candidate2 = line[line.size - 2]
        val count1 = getPointsWithinDistance(points, candidate1, PARTICLE_DETECTION_TOLERANCE)
        val count2 = getPointsWithinDistance(points, candidate2, PARTICLE_DETECTION_TOLERANCE)

        // One should be 2 (base) and the other 4 (tip)
        if (setOf(count1, count2) != setOf(COUNT_NEAR_BASE, COUNT_NEAR_TIP)) return null

        val base: LorenzVec
        val tip: LorenzVec

        if (count1 == COUNT_NEAR_TIP) { // if the first point is the base
            tip = line.first()
            base = line.last()
        } else {
            tip = line.last()
            base = line.first()
        }

        // arrow is a block above the center of the start block
        val adjustedBase = base.down(1.5) // this is always an exact multiple of 0.5
        val adjustedTip = tip.down(1.5)

        BurrowApi.lastBurrowInteracted?.let { if (adjustedBase.distanceSq(it) > 5) return null } // not your arrow

        return RaycastUtils.Ray(adjustedBase, adjustedTip.minus(adjustedBase).normalize())
    }

    private fun getPointsWithinDistance(
        points: Iterable<LorenzVec>,
        origin: LorenzVec,
        maxDist: Double,
    ): Int {
        val maxDistSq = maxDist * maxDist
        return points.count { it != origin && it.distanceSq(origin) <= maxDistSq }
    }

    private fun findLine(points: Iterable<LorenzVec>, shaftLength: Int, maxDist: Double): List<LorenzVec> {
        for (point in points) {
            val line = mutableListOf<LorenzVec>()
            val visited = mutableSetOf<LorenzVec>()
            line.add(point)
            visited.add(point)

            if (extendLine(line, visited, points, shaftLength, maxDist)) {
                return line.toList()
            }
        }
        return emptyList()
    }

    private fun extendLine(
        line: MutableList<LorenzVec>,
        visited: MutableSet<LorenzVec>,
        points: Iterable<LorenzVec>,
        numPoints: Int,
        maxDist: Double,
    ): Boolean {
        if (line.size == numPoints) return true // line completed

        var nextPoint: LorenzVec? = null
        var minDist = Double.MAX_VALUE

        for (point in points) {
            if (visited.contains(point)) continue
            val dist = line.last().distance(point)
            if (dist > maxDist) continue

            // must be collinear with all existing points
            val second = if (line.size > 1) line[1] else line[0]
            if (!isCollinear(line.first(), second, point)) continue

            if (dist < minDist) {
                minDist = dist
                nextPoint = point
            }
        }

        if (nextPoint != null) {
            line.add(nextPoint)
            visited.add(nextPoint)
            if (extendLine(line, visited, points, numPoints, maxDist)) {
                return true
            }
            // backtrack
            line.removeLast()
            visited.remove(nextPoint)
        }

        return false
    }

    private fun isCollinear(a: LorenzVec, b: LorenzVec, c: LorenzVec): Boolean {
        val ab = b.minus(a)
        val ac = c.minus(a)
        val cross = ab.crossProduct(ac)
        return cross.lengthSquared() < EPSILON
    }

    private fun isEnabled() = DianaApi.isDoingDiana() && config.guess && config.guessFromArrow
}
