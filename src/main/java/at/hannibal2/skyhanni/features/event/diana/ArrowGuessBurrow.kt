package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.diana.BurrowDugEvent
import at.hannibal2.skyhanni.events.diana.BurrowGuessEvent
import at.hannibal2.skyhanni.features.misc.CurrentPing
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RaycastUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.VectorUtils.down
import at.hannibal2.skyhanni.utils.VectorUtils.minus
import at.hannibal2.skyhanni.utils.VectorUtils.roundToBlock
import at.hannibal2.skyhanni.utils.VectorUtils.toDoubleArray
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sign
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ArrowGuessBurrow {
    private val config get() = SkyHanniMod.feature.event.diana

    private const val SHAFT_LENGTH = 20
    private const val PARTICLE_DETECTION_TOLERANCE = 0.12
    private const val PARTICLE_DETECTION_TOLERANCE_SQ =
        PARTICLE_DETECTION_TOLERANCE * PARTICLE_DETECTION_TOLERANCE
    private const val COUNT_NEAR_TIP = 4
    private const val COUNT_NEAR_BASE = 2
    private const val EPSILON = 1e-6

    private val points = mutableSetOf<Vec3>()
    private val recentFoundArrows = TimeLimitedSet<RaycastUtils.Ray>(18.seconds)
    var lastArrowTime = SimpleTimeMark.farPast()

    private var failures = 0

    @HandleEvent(onlyOnIsland = IslandType.HUB, receiveCancelled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return

        if (event.distanceToPlayer > 6) return
        if (event.type != ParticleTypes.DUST) return
        if (event.count != 0) return
        if (event.speed != 1.0f) return

        // offset is color for some reason
        val range = getArrowRange(event.offset) ?: return

        DelayedRun.runOrNextTick {
            points.add(event.location)
            detectArrow()?.let {
                val dugBlock = it.origin.roundToBlock()
                GriffinBurrowHelper.addDebug("detected arrow origin above block [${dugBlock.x}, ${dugBlock.y}, ${dugBlock.z}]")
                GriffinBurrowHelper.removeGuess(dugBlock, "origin of detected arrow")
                DelayedRun.runDelayed(CurrentPing.averagePing + 200.milliseconds) {
                    GriffinBurrowHelper.removeGuess(dugBlock, "origin of detected arrow (delayed)")
                }
                lastArrowTime = SimpleTimeMark.now()
                addGuessFromRay(it, range) ?: run {
                    GriffinBurrowHelper.addDebug("arrow guess returned null")
                    failures++
                    if (config.warnOnFail) {
                        GriffinBurrowHelper.showUseSpadeTitle()
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        if (event.current != event.max) {
            points.clear()
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

    fun getArrowRange(offset: Vec3): IntRange? = when (offset) {
        Vec3(0.0, 128.0, 0.0) -> IntRange(0, 117) // yellow
        Vec3(255.0, 255.0, 0.0) -> IntRange(112, 282) // red
        Vec3(255.0, 0.0, 0.0) -> IntRange(281, 600) // black
        else -> null
    }

    @Suppress("ReturnCount")
    private fun addGuessFromRay(ray: RaycastUtils.Ray, range: IntRange): Vec3? {
        val bounds = IslandType.HUB.islandData?.boundingBox ?: run {
            GriffinBurrowHelper.addDebug("couldnt get hub bounds")
            return null
        }
        if (!bounds.contains(ray.origin)) { // guarantees exit point is first intersect
            GriffinBurrowHelper.addDebug("origin not in bounds")
            return null
        }
        // you technically don't need to find the endpoint for this, but it makes it simpler so why not
        val endPoint = RaycastUtils.intersectAABBWithRay(bounds, ray)?.second ?: run {
            GriffinBurrowHelper.addDebug("couldnt find endpoint")
            return null
        }

        val diff = (endPoint - ray.origin).toDoubleArray()
        val axisIndex = diff.withIndex()
            .filter { (_, value) -> abs(value) > 0.9 } // only if the axis isn't the same block
            .minByOrNull { (_, value) -> abs(value) } // find the axis with the least change
            ?.index
            ?: run {
                GriffinBurrowHelper.addDebug("couldnt find axis index")
                return null
            }

        // position mapped to scaledDistToRay and distFromOrigin
        val candidates = mutableMapOf<Vec3, Pair<Double, Double>>()
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

            val distanceFromOrigin = candidatePoint.distanceTo(ray.origin)

            // take the ratio to account for errors
            val scaledDistance = (distanceToRay * 500000 / distanceFromOrigin)

            candidates[candidateBlock] = Pair(scaledDistance.roundTo(2), distanceFromOrigin)
        }

        if (candidates.isEmpty()) {
            GriffinBurrowHelper.addDebug("candidates is empty")
            return null
        }
        val minValue = candidates.values.minOf { it.first }
        val possibilities = candidates.filterValues { it.first == minValue }
        val withinRange = possibilities.filterValues { it.second.toInt() in range }.map { it.key }
        if (withinRange.isEmpty()) {
            GriffinBurrowHelper.addDebug("no candidates within range")
            return null
        }

        BurrowGuessEvent(GuessEntry(withinRange), "arrow guess").post()

        return withinRange[0]
    }

    private fun detectArrow(): RaycastUtils.Ray? {
        val line = findLine()
        if (line.isEmpty()) return null

        // the head of the arrow intersects with the particle one off the end
        // findLine only returns a full line with 20 points or an emptyList so this is safe
        val candidate1 = line[1]
        val candidate2 = line[line.size - 2]
        val count1 = getPointsWithinDistance(candidate1)
        val count2 = getPointsWithinDistance(candidate2)

        // One should be 2 (base) and the other 4 (tip)
        if (setOf(count1, count2) != setOf(COUNT_NEAR_BASE, COUNT_NEAR_TIP)) return null

        val base: Vec3
        val tip: Vec3

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

        val ray = RaycastUtils.Ray(adjustedBase, adjustedTip.minus(adjustedBase).normalize())
        if (recentFoundArrows.add(ray)) return null
        points.clear()

        // not your arrow
        if (BurrowApi.lastBurrowRelatedChatMessage.passedSince() > 500.milliseconds) {
            val playerLocation = LocationUtils.playerLocation()
            val bStr = "[${adjustedBase.roundToBlock().x}, ${adjustedBase.roundToBlock().y}, ${adjustedBase.roundToBlock().z}]"
            val pStr = "[${playerLocation.x}, ${playerLocation.y}, ${playerLocation.z}]"
            GriffinBurrowHelper.addDebug("not your arrow detected at $bStr, player pos $pStr")
            return null
        }

        return ray
    }

    private fun getPointsWithinDistance(origin: Vec3): Int =
        points.count { it != origin && it.distanceToSqr(origin) <= PARTICLE_DETECTION_TOLERANCE_SQ }

    private fun findLine(): List<Vec3> {
        for (point in points) {
            val line = mutableListOf<Vec3>()
            val visited = mutableSetOf<Vec3>()
            line.add(point)
            visited.add(point)

            if (extendLine(line, visited, points, SHAFT_LENGTH, PARTICLE_DETECTION_TOLERANCE)) {
                return line.toList()
            }
        }
        return emptyList()
    }

    private fun extendLine(
        line: MutableList<Vec3>,
        visited: MutableSet<Vec3>,
        points: Iterable<Vec3>,
        numPoints: Int,
        maxDist: Double,
    ): Boolean {
        if (line.size == numPoints) return true // line completed

        var nextPoint: Vec3? = null
        var minDist = Double.MAX_VALUE

        for (point in points) {
            if (visited.contains(point)) continue
            val dist = line.last().distanceTo(point)
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

    private fun isCollinear(a: Vec3, b: Vec3, c: Vec3): Boolean =
        (b - a).cross(c - a).lengthSqr() < EPSILON

    private fun isEnabled() = DianaApi.isDoingDiana() && config.guess && config.guessFromArrow
}
