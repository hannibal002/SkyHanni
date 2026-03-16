package at.hannibal2.skyhanni.data.navigation

import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.GraphUtils.playerPosition
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.addWaters
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DBezier2
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import java.awt.Color

/**
 * TODO
 *
 * IslandGraphs: rename all functions and members to be more logical/explain what they do
 * IslandGraphs: magic numbers
 * IslandGraphs: fix distance calculation for display being off
 *
 * arrow in direction in 2d user frame space: if close to player point is out of player looking direction/frustum
 *
 * show distance to target 5 blocks in front of path
 * option to show x more worse paths, in different colors, only the ones that actually have a different
 * node structure around the 5 blocks in front of you
 *
 * if the closest node and the path to it is not visible, move further down the line of currently moves paths to
 *  find the spot where to start the curve from. and if that also doesnt work, find a new edge on the actual graph to start the path on.
 *
 *  do not jump forward if the path is higher than the user location
 *
 *  fix the rendering  being weird when moving up a ladder, same issue when diving
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

private const val STANDING_EYE_HEIGHT = 1.62

// distance in blocks above/below a water surface crossing where depth testing is disabled
private const val PEEK_DISTANCE = 4.0

private val waterBlocks = buildList { addWaters() }

private fun LorenzVec.isWater(): Boolean = getBlockAt() in waterBlocks

private class DensePoint(val pos: LorenzVec, val isWater: Boolean) {
    // depth testing disabled near a water surface crossing so the line renders through water
    var isPeek: Boolean = false
}

private data class CurveEnd(val pos: LorenzVec, val tangent: LorenzVec, val nextIdx: Int)

/**
 * Uses tick and render events to calculate the final pathfind lines.
 */
class PathRenderer(val path: Graph, private val color: Color, private val targetLocation: LorenzVec) {

    private val densePoints: List<DensePoint> = subdividePositions(path.map { it.position.addHalf() }).map { DensePoint(it, it.isWater()) }
    private var curveMaxDist: Double = 0.0

    fun render(event: SkyHanniRenderWorldEvent) {
        renderCurve(event)
        val lastNode = path.lastOrNull()?.position ?: return
        event.draw3DLine(lastNode.addHalf(), targetLocation.addHalf(), color, FAR_LINE_WIDTH, !densePoints.last().isPeek)
        event.drawWaypointFilled(targetLocation, color, seeThroughBlocks = true)
    }

    private fun renderCurve(event: SkyHanniRenderWorldEvent) {
        val eyePos = event.exactPlayerEyeLocation()
        val anchorY = eyePos.y - MinecraftCompat.localPlayer.eyeHeight + STANDING_EYE_HEIGHT
        if (densePoints.isEmpty()) return

        if (densePoints.size == 1) {
            renderSingleNodeCurve(event, eyePos, anchorY, densePoints[0])
            return
        }

        val (startPos, nextDenseIdx) = projectOntoPath(eyePos)
        val walkPositions: List<LorenzVec> = listOf(startPos) + densePoints.drop(nextDenseIdx).map { it.pos }
        val curveEnd = walkToEnd(walkPositions, nextDenseIdx) ?: return

        val dirToCurve = (curveEnd.pos - eyePos).normalize()
        val anchor = LorenzVec(eyePos.x, anchorY + ANCHOR_Y_OFFSET, eyePos.z) + dirToCurve * ANCHOR_FORWARD_DIST
        val scale = anchor.distance(curveEnd.pos) * CONTROL_POINT_SCALE
        val controlPoint = curveEnd.pos - curveEnd.tangent * scale
        val bezierDepth = !WorldRenderUtils.isRenderingUnderwater()
        event.draw3DBezier2(anchor, controlPoint, curveEnd.pos, color, NEAR_LINE_WIDTH, bezierDepth)
        if (curveEnd.nextIdx > densePoints.lastIndex) return

        val firstFar = densePoints[curveEnd.nextIdx]
        event.draw3DLine(curveEnd.pos, firstFar.pos, color, NEAR_LINE_WIDTH, bezierDepth && !firstFar.isPeek)
        for (i in curveEnd.nextIdx until densePoints.lastIndex) {
            val a = densePoints[i]
            val b = densePoints[i + 1]
            event.draw3DLine(a.pos, b.pos, color, NEAR_LINE_WIDTH, !a.isPeek && !b.isPeek)
        }
    }

    private fun renderSingleNodeCurve(event: SkyHanniRenderWorldEvent, eyePos: LorenzVec, anchorY: Double, point: DensePoint) {
        val nodePos = point.pos
        val dirToNode = (nodePos - eyePos).normalize()
        val anchor = LorenzVec(eyePos.x, anchorY + ANCHOR_Y_OFFSET, eyePos.z) + dirToNode * ANCHOR_FORWARD_DIST
        val scale = anchor.distance(nodePos) * CONTROL_POINT_SCALE
        val controlPoint = nodePos - dirToNode * scale
        event.draw3DBezier2(anchor, controlPoint, nodePos, color, NEAR_LINE_WIDTH, !WorldRenderUtils.isRenderingUnderwater())
    }

    private fun walkTangent(walkPositions: List<LorenzVec>, startSegIdx: Int, startPos: LorenzVec): LorenzVec {
        var remaining = TANGENT_LOOKAHEAD
        var prev = startPos
        for (i in startSegIdx until walkPositions.size) {
            val next = walkPositions[i]
            val d = prev.distance(next)
            if (d >= remaining) {
                return (prev + (next - prev).normalize() * remaining - startPos).normalize()
            }
            remaining -= d
            prev = next
        }
        return if (prev.distanceSq(startPos) > 0.0001) (prev - startPos).normalize()
        else (walkPositions.last() - walkPositions[walkPositions.lastIndex - 1]).normalize()
    }

    private fun walkToEnd(walkPositions: List<LorenzVec>, nextDenseIdx: Int): CurveEnd? {
        var totalDist = 0.0
        var result: CurveEnd? = null
        for (i in 1..walkPositions.lastIndex) {
            val segStart = walkPositions[i - 1]
            val segEnd = walkPositions[i]
            val segLen = segStart.distance(segEnd)
            val remaining = curveMaxDist - totalDist
            if (segLen >= remaining) {
                val endPos = segStart + (segEnd - segStart).normalize() * remaining
                return CurveEnd(endPos, walkTangent(walkPositions, i, endPos), nextDenseIdx + i - 1)
            }
            totalDist += segLen
            result = CurveEnd(segEnd, (segEnd - segStart).normalize(), nextDenseIdx + i - 1)
        }
        return result
    }

    private fun projectOntoPath(eyePos: LorenzVec): Pair<LorenzVec, Int> {
        var bestDistSq = Double.MAX_VALUE
        var bestPos = densePoints[0].pos
        var bestNextIdx = 1
        for (i in 0 until densePoints.lastIndex) {
            val proj = eyePos.nearestPointOnLine(densePoints[i].pos, densePoints[i + 1].pos)
            val distSq = eyePos.distanceSq(proj)
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestPos = proj
                bestNextIdx = i + 1
            }
        }
        return bestPos to bestNextIdx
    }

    fun updateNearSegment() {
        val dense = densePoints
        for (point in dense) point.isPeek = false
        val closestIdx = findClosestIndex(dense, playerPosition)
        var totalDist = 0.0
        for (i in (closestIdx + 1)..dense.lastIndex) {
            if (!dense[i].pos.canBeSeen()) break
            totalDist += dense[i - 1].pos.distance(dense[i].pos)
            if (totalDist >= CURVE_RADIUS) {
                totalDist = CURVE_RADIUS; break
            }
        }
        curveMaxDist = totalDist.coerceAtLeast(SUBDIVISION_STEP)
        val peekSteps = (PEEK_DISTANCE / SUBDIVISION_STEP).toInt()
        for (i in maxOf(0, closestIdx - 1) until dense.lastIndex) {
            if (dense[i].isWater == dense[i + 1].isWater) continue
            if (!dense[i].pos.canBeSeen()) break
            val peekStart = maxOf(0, i + 1 - peekSteps)
            val peekEnd = minOf(dense.lastIndex, i + 1 + peekSteps)
            for (j in peekStart..peekEnd) dense[j].isPeek = true
        }
    }

    private fun catmullRomPoint(p0: LorenzVec, p1: LorenzVec, p2: LorenzVec, p3: LorenzVec, t: Double): LorenzVec {
        val t2 = t * t
        val t3 = t2 * t
        val a = p1 * 2.0
        val b = (p2 - p0) * t
        val c = (p0 * 2.0 - p1 * 5.0 + p2 * 4.0 - p3) * t2
        val d = (p1 * 3.0 - p0 - p2 * 3.0 + p3) * t3
        return (a + b + c + d) * 0.5
    }

    private fun subdividePositions(positions: List<LorenzVec>): List<LorenzVec> {
        if (positions.size < 2) return positions
        val result = mutableListOf<LorenzVec>()
        result.add(positions.first())
        for (i in 0 until positions.lastIndex) {
            val p0 = positions.getOrElse(i - 1) { positions[i] }
            val p1 = positions[i]
            val p2 = positions[i + 1]
            val p3 = positions.getOrElse(i + 2) { positions[i + 1] }
            val steps = (p1.distance(p2) / SUBDIVISION_STEP).toInt().coerceAtLeast(1)
            for (step in 1..steps) {
                result.add(catmullRomPoint(p0, p1, p2, p3, step.toDouble() / steps))
            }
        }
        return result
    }

    private fun findClosestIndex(positions: List<DensePoint>, referencePos: LorenzVec): Int =
        positions.indices.minBy { positions[it].pos.distance(referencePos) }
}
