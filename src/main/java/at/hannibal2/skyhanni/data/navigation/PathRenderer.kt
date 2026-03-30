package at.hannibal2.skyhanni.data.navigation

import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.GraphUtils.playerPosition
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.PlayerUtils.STANDING_EYE_HEIGHT
import at.hannibal2.skyhanni.utils.VectorUtils.minus
import at.hannibal2.skyhanni.utils.VectorUtils.nearestPointOnLine
import at.hannibal2.skyhanni.utils.VectorUtils.plus
import at.hannibal2.skyhanni.utils.VectorUtils.times
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.addWaters
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DBezier2
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import net.minecraft.world.phys.Vec3
import java.awt.Color

/**
 * TODO
 *
 * arrow in direction in 2d user frame space: if close to player point is out of player looking direction/frustum
 *
 * show distance to target 5 blocks in front of path
 * option to show x more worse paths, in different colors, only the ones that actually have a different
 * node structure around the 5 blocks in front of you
 *
 * if the closest node and the path to it is not visible, move further down the line of currently moves paths to
 *  find the spot where to start the curve from. and if that also doesn't work, find a new edge on the actual graph to start the path on.
 *
 *  do not jump forward if the path is higher than the user location
 *
 *  fix the rendering being weird when moving up a ladder, same issue when diving
 */

private const val SUBDIVISION_STEP = 0.5
private const val CURVE_RADIUS = 8.0

private const val ANCHOR_Y_OFFSET = -1.0
private const val ANCHOR_FORWARD_DIST = 0.7

private const val CONTROL_POINT_SCALE = 0.5

// blocks to look ahead along the path when computing the bezier tangent at the curve endpoint
private const val TANGENT_LOOKAHEAD = 1.5

private const val NEAR_LINE_WIDTH = 6
private const val FAR_LINE_WIDTH = 4

// distance in blocks above/below a water surface crossing where depth testing is disabled
private const val PEEK_DISTANCE = 4.0

private val waterBlocks = buildList { addWaters() }

private fun Vec3.isWater(): Boolean = getBlockAt() in waterBlocks

private class PathPoint(val pos: Vec3, val isWater: Boolean) {
    // depth testing disabled near a water surface crossing so the line renders through water
    var isPeek: Boolean = false
}

private data class CurveEnd(val pos: Vec3, val tangent: Vec3, val nextIdx: Int)

/**
 * Uses tick and render events to calculate the final pathfind lines.
 */
class PathRenderer(val path: Graph, private val color: Color, private val targetLocation: Vec3) {

    private val pathPoints: List<PathPoint> =
        subdividePositions(path.map { it.position.add(0.5) }).map { PathPoint(it, it.isWater()) }
    private var nearCurveLength: Double = 0.0
    var remainingDistance: Double = 0.0
        private set

    fun render(event: SkyHanniRenderWorldEvent) {
        renderPathSegments(event)
        val lastNode = path.lastOrNull()?.position ?: return
        event.draw3DLine(lastNode.add(0.5), targetLocation.add(0.5), color, FAR_LINE_WIDTH, !pathPoints.last().isPeek)
        event.drawWaypointFilled(targetLocation, color, seeThroughBlocks = true)
    }

    private fun renderPathSegments(event: SkyHanniRenderWorldEvent) {
        val eyePos = event.exactPlayerEyeLocation()
        val anchorY = eyePos.y - MinecraftCompat.localPlayer.eyeHeight + STANDING_EYE_HEIGHT
        if (pathPoints.isEmpty()) return

        if (pathPoints.size == 1) {
            renderSingleNodeCurve(event, eyePos, anchorY, pathPoints[0])
            return
        }

        val (startPos, nextPathIdx) = projectOntoPath(eyePos)
        val walkPositions: List<Vec3> = listOf(startPos) + pathPoints.drop(nextPathIdx).map { it.pos }
        val curveEnd = findBezierEnd(walkPositions, nextPathIdx) ?: return

        val dirToCurve = (curveEnd.pos - eyePos).normalize()
        val anchor = Vec3(eyePos.x, anchorY + ANCHOR_Y_OFFSET, eyePos.z) + dirToCurve * ANCHOR_FORWARD_DIST
        val scale = anchor.distanceTo(curveEnd.pos) * CONTROL_POINT_SCALE
        val controlPoint = curveEnd.pos - curveEnd.tangent * scale
        val bezierDepth = !WorldRenderUtils.isRenderingUnderwater()
        event.draw3DBezier2(anchor, controlPoint, curveEnd.pos, color, NEAR_LINE_WIDTH, bezierDepth)
        if (curveEnd.nextIdx > pathPoints.lastIndex) return

        val firstFar = pathPoints[curveEnd.nextIdx]
        event.draw3DLine(curveEnd.pos, firstFar.pos, color, NEAR_LINE_WIDTH, bezierDepth && !firstFar.isPeek)
        for (i in curveEnd.nextIdx until pathPoints.lastIndex) {
            val a = pathPoints[i]
            val b = pathPoints[i + 1]
            event.draw3DLine(a.pos, b.pos, color, NEAR_LINE_WIDTH, !a.isPeek && !b.isPeek)
        }
    }

    private fun renderSingleNodeCurve(
        event: SkyHanniRenderWorldEvent,
        eyePos: Vec3,
        anchorY: Double,
        point: PathPoint,
    ) {
        val nodePos = point.pos
        val dirToNode = (nodePos - eyePos).normalize()
        val anchor = Vec3(eyePos.x, anchorY + ANCHOR_Y_OFFSET, eyePos.z) + dirToNode * ANCHOR_FORWARD_DIST
        val scale = anchor.distanceTo(nodePos) * CONTROL_POINT_SCALE
        val controlPoint = nodePos - dirToNode * scale
        event.draw3DBezier2(
            p1 = anchor,
            control = controlPoint,
            p3 = nodePos,
            color = color,
            lineWidth = NEAR_LINE_WIDTH,
            depth = !WorldRenderUtils.isRenderingUnderwater(),
        )
    }

    private fun walkTangent(walkPositions: List<Vec3>, startSegIdx: Int, startPos: Vec3): Vec3 {
        var remaining = TANGENT_LOOKAHEAD
        var prev = startPos
        for (i in startSegIdx until walkPositions.size) {
            val next = walkPositions[i]
            val d = prev.distanceTo(next)
            if (d >= remaining) {
                return (prev + (next - prev).normalize() * remaining - startPos).normalize()
            }
            remaining -= d
            prev = next
        }
        return if (prev.distanceToSqr(startPos) > 0.0001) (prev - startPos).normalize()
        else (walkPositions.last() - walkPositions[walkPositions.lastIndex - 1]).normalize()
    }

    private fun findBezierEnd(walkPositions: List<Vec3>, nextPathIdx: Int): CurveEnd? {
        var totalDist = 0.0
        var result: CurveEnd? = null
        for (i in 1..walkPositions.lastIndex) {
            val segStart = walkPositions[i - 1]
            val segEnd = walkPositions[i]
            val segLen = segStart.distanceTo(segEnd)
            val remaining = nearCurveLength - totalDist
            if (segLen >= remaining) {
                val endPos = segStart + (segEnd - segStart).normalize() * remaining
                return CurveEnd(endPos, walkTangent(walkPositions, i, endPos), nextPathIdx + i - 1)
            }
            totalDist += segLen
            result = CurveEnd(segEnd, (segEnd - segStart).normalize(), nextPathIdx + i - 1)
        }
        return result
    }

    private fun projectOntoPath(eyePos: Vec3): Pair<Vec3, Int> {
        var bestDistSq = Double.MAX_VALUE
        var bestPos = pathPoints[0].pos
        var bestNextIdx = 1
        for (i in 0 until pathPoints.lastIndex) {
            val proj = eyePos.nearestPointOnLine(pathPoints[i].pos, pathPoints[i + 1].pos)
            val distSq = eyePos.distanceToSqr(proj)
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestPos = proj
                bestNextIdx = i + 1
            }
        }
        return bestPos to bestNextIdx
    }

    fun updateNearSegment() {
        for (point in pathPoints) point.isPeek = false
        val closestIdx = findClosestIndex(pathPoints, playerPosition)
        remainingDistance = calculateDistance(closestIdx)

        var totalDist = 0.0
        for (i in (closestIdx + 1)..pathPoints.lastIndex) {
            if (!pathPoints[i].pos.canBeSeen()) break
            totalDist += pathPoints[i - 1].pos.distanceTo(pathPoints[i].pos)
            if (totalDist >= CURVE_RADIUS) {
                totalDist = CURVE_RADIUS
                break
            }
        }
        nearCurveLength = totalDist.coerceAtLeast(SUBDIVISION_STEP)
        val peekSteps = (PEEK_DISTANCE / SUBDIVISION_STEP).toInt()
        for (i in maxOf(0, closestIdx - 1) until pathPoints.lastIndex) {
            if (pathPoints[i].isWater == pathPoints[i + 1].isWater) continue
            if (!pathPoints[i].pos.canBeSeen()) break
            val peekStart = maxOf(0, i + 1 - peekSteps)
            val peekEnd = minOf(pathPoints.lastIndex, i + 1 + peekSteps)
            for (j in peekStart..peekEnd) pathPoints[j].isPeek = true
        }
    }

    private fun calculateDistance(closestIdx: Int): Double {
        var distance = pathPoints[closestIdx].pos.distanceTo(playerPosition)
        for (i in closestIdx until pathPoints.lastIndex) {
            distance += pathPoints[i].pos.distanceTo(pathPoints[i + 1].pos)
        }
        return distance + pathPoints.last().pos.distanceTo(targetLocation.add(0.5))
    }

    fun nearestPathDistanceSq(): Double =
        pathPoints.minOfOrNull { it.pos.distanceSqToPlayer() } ?: Double.MAX_VALUE

    private fun catmullRomPoint(p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3, t: Double): Vec3 {
        val t2 = t * t
        val t3 = t2 * t
        val a = p1 * 2.0
        val b = (p2 - p0) * t
        val c = (p0 * 2.0 - p1 * 5.0 + p2 * 4.0 - p3) * t2
        val d = (p1 * 3.0 - p0 - p2 * 3.0 + p3) * t3
        return (a + b + c + d) * 0.5
    }

    private fun subdividePositions(positions: List<Vec3>): List<Vec3> {
        if (positions.size < 2) return positions
        val result = mutableListOf<Vec3>()
        result.add(positions.first())
        for (i in 0 until positions.lastIndex) {
            val p0 = positions.getOrElse(i - 1) { positions[i] }
            val p1 = positions[i]
            val p2 = positions[i + 1]
            val p3 = positions.getOrElse(i + 2) { positions[i + 1] }
            val steps = (p1.distanceTo(p2) / SUBDIVISION_STEP).toInt().coerceAtLeast(1)
            for (step in 1..steps) {
                result.add(catmullRomPoint(p0, p1, p2, p3, step.toDouble() / steps))
            }
        }
        return result
    }

    private fun findClosestIndex(positions: List<PathPoint>, referencePos: Vec3): Int =
        positions.indices.minBy { positions[it].pos.distanceTo(referencePos) }
}
