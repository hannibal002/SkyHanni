package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.diana.BurrowGuessEvent
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowHelper.allowedBlocksAboveGround
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RaycastUtils
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import net.minecraft.init.Blocks
import net.minecraft.util.EnumParticleTypes
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object ArrowGuessBurrow {
    private val config get() = SkyHanniMod.feature.event.diana

    private var points: HashSet<LorenzVec> = HashSet()
    private var recentArrowParticles = TimeLimitedSet<LorenzVec>(1.minutes)

    @HandleEvent(onlyOnIsland = IslandType.HUB, receiveCancelled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return

        if (event.distanceToPlayer > 6) return
        if (event.type != EnumParticleTypes.REDSTONE) return
        if (event.count != 0) return
        if (event.speed != 1.0f) return

        val allowedOffsets = setOf(0.0, 128.0, 255.0)
        if (event.offset.x !in allowedOffsets) return
        if (event.offset.y !in allowedOffsets) return
        if (event.offset.z !in allowedOffsets) return

        if (recentArrowParticles.contains(event.location)) return
        recentArrowParticles.add(event.location)
        points.add(event.location)

        val arrow = detectArrow(points) ?: return
        val guess = findClosestValidBlockToRay(arrow) ?: return

        // if you dig a burrow while its tracking particles it doesn't create a new waypoint I think but this is rare and non-fatal
        GriffinBurrowHelper.newBurrow = false
        BurrowGuessEvent(
            guess,
            precise = true,
            new = true
        ).post()

        points.clear()
    }

    private fun findClosestValidBlockToRay(ray: RaycastUtils.Ray): LorenzVec? {
        val bounds = IslandType.HUB.islandData?.boundingBox
        val step = 0.9

        if (bounds == null) return null

        var closest: LorenzVec? = null
        var closestDistance = Double.MAX_VALUE

        val seen = HashSet<LorenzVec>()
        seen.add(ray.origin.roundToBlock())

        // travel the ray
        var iteration = 0
        while (true) {
            if (iteration > 2000) break
            val current = ray.origin + ray.direction.times(step * iteration)
            if (!bounds.isInside(current)) break

            val pos = current.roundToBlock()
            val center = pos.add(0.5,0.5,0.5)

            if (seen.add(pos) && isBlockValid(pos)) {
                val distance = RaycastUtils.findDistanceToRay(ray, center)
                if (closest == null || closestDistance > distance) {
                    closest = pos
                    closestDistance = distance
                }
            }
            iteration++
        }

        return closest
    }

    private fun isBlockValid(pos: LorenzVec): Boolean {
        if (!pos.isInLoadedChunk()) {
            return true
        }
        val isGround = pos.copy().getBlockAt() == Blocks.grass
        val isValidBlockAbove = pos.copy(y = pos.y + 1).getBlockAt() in allowedBlocksAboveGround
        return isGround && isValidBlockAbove
    }

    private fun detectArrow(points: HashSet<LorenzVec>): RaycastUtils.Ray? {
        val shaftLength = 20
        val maxDist = 0.12

        val line = findLine(points, shaftLength, maxDist)
        if (line.isEmpty()) return null


        // the head of the arrow intersects with the particle one off the end, findLine only returns a full line with 20 points or an emptyList so this is safe
        val candidate1 = line[1]
        val candidate2 = line[line.size - 2]
        val count1 = getPointsWithinDistance(points, candidate1, maxDist)
        val count2 = getPointsWithinDistance(points, candidate2, maxDist)

        // One should be 2 (base) and the other 4 (tip)
        if (!((count1 == 2 && count2 == 4) || (count1 == 4 && count2 == 2))) return null

        val base: LorenzVec
        val tip: LorenzVec

        if (count1 == 4) { // if the first point is the base
            tip = line.first()
            base = line.last()
        } else {
            tip = line.last()
            base = line.first()
        }

        // arrow is a block above the center of the start block
        val adjustedBase = base.minus(LorenzVec(0.0,1.5,0.0))
        val adjustedTip = tip.minus(LorenzVec(0.0,1.5,0.0))

        return RaycastUtils.Ray(adjustedBase, adjustedTip.minus(adjustedBase).normalize())
    }

    private fun getPointsWithinDistance(
        points: Iterable<LorenzVec>,
        origin: LorenzVec,
        maxDist: Double
    ): Int {
        val maxDistSq = maxDist * maxDist
        var count = 0

        for (point in points) {
            // exclude the origin itself
            if (point == origin) continue

            val distSq = point.distanceSq(origin)
            if (distSq <= maxDistSq) {
                count++
            }
        }

        return count
    }

    private fun findLine(points: HashSet<LorenzVec>, shaftLength: Int, maxDist: Double): List<LorenzVec> {
        for (point in points) {
            val line = ArrayList<LorenzVec>(shaftLength)
            val visited = HashSet<LorenzVec>()
            line.add(point)
            visited.add(point)

            if (extendLine(line, visited, points, shaftLength, maxDist)) {
                return line
            }
        }
        return emptyList()
    }

    private fun extendLine(
        line: MutableList<LorenzVec>,
        visited: MutableSet<LorenzVec>,
        points: Set<LorenzVec>,
        numPoints: Int,
        maxDist: Double
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
        val epsilon = 1e-6
        return cross.lengthSquared() < epsilon
    }

    //TODO onDebug or whatever

    private fun isEnabled() = DianaApi.isDoingDiana() && config.guess && config.guessFromArrow
}
