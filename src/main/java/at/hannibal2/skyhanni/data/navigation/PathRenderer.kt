package at.hannibal2.skyhanni.data.navigation

import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.GraphUtils.playerPosition
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.addWaters
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DBezier2
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import java.awt.Color

/**
 * TODO
 *
 * bug: sometimes disappears entirely for a frame
 * bug: does not show up immediately on start, only after node move
 * improvement: corners are too sharp, smooth them
 * block at target is gone
 * jump all 2 blocks/per node
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
 *  do not jump forward if the path is  higher than the user location
 *
 *  fix the rendering  being weird when moving up a ladder, same issue when diving
 */

private const val SUBDIVISION_STEP = 0.5
private const val CURVE_RADIUS = 8.0

private const val ANCHOR_Y_OFFSET = -1.0
private const val ANCHOR_FORWARD_DIST = 0.7

private const val CONTROL_POINT_SCALE = 0.5

private const val NEAR_LINE_WIDTH = 6
private const val FAR_LINE_WIDTH = 4

private const val STANDING_EYE_HEIGHT = 1.62

private val waterBlocks = buildList { addWaters() }

private fun LorenzVec.isWater(): Boolean = getBlockAt() in waterBlocks

private data class DensePoint(val pos: LorenzVec, val isWater: Boolean)

/**
 * Uses tick and render events to calculate the final pathfind lines.
 */
class PathRenderer(val path: Graph, private val color: Color, private val targetLocation: LorenzVec) {

    private val densePoints: List<DensePoint> = subdividePositions(path.map { it.position.addHalf() }).map { DensePoint(it, it.isWater()) }
    private val targetIsWater: Boolean = targetLocation.addHalf().isWater()
    private var curveMaxDist: Double = 0.0

    fun render(event: SkyHanniRenderWorldEvent) {
        renderCurve(event)
        val lastNode = path.lastOrNull()?.position ?: return
        val eyeIsWater = MinecraftCompat.localPlayer.isInWater
        event.draw3DLine(lastNode.addHalf(), targetLocation.addHalf(), color, FAR_LINE_WIDTH, !eyeIsWater && !densePoints.last().isWater && !targetIsWater)
    }

    private fun renderCurve(event: SkyHanniRenderWorldEvent) {
        val eyePos = event.exactPlayerEyeLocation()
        val eyeIsWater = MinecraftCompat.localPlayer.isInWater
        val anchorY = eyePos.y - MinecraftCompat.localPlayer.eyeHeight + STANDING_EYE_HEIGHT
        val dense = densePoints
        val maxDist = curveMaxDist
        if (dense.isEmpty()) return

        if (dense.size == 1) {
            val point = dense[0]
            val nodePos = point.pos
            val dirToNode = (nodePos - eyePos).normalize()
            val anchor = LorenzVec(eyePos.x, anchorY + ANCHOR_Y_OFFSET, eyePos.z) + dirToNode * ANCHOR_FORWARD_DIST
            val scale = anchor.distance(nodePos) * CONTROL_POINT_SCALE
            val controlPoint = nodePos - dirToNode * scale
            event.draw3DBezier2(anchor, controlPoint, nodePos, color, NEAR_LINE_WIDTH, !eyeIsWater && !point.isWater)
            return
        }

        val (startPos, nextDenseIdx) = projectOntoPath(dense, eyePos)
        val walkPositions: List<LorenzVec> = listOf(startPos) + dense.drop(nextDenseIdx).map { it.pos }
        var totalDist = 0.0
        var curveEndPos: LorenzVec? = null
        var curveTangent = LorenzVec(0.0, 0.0, 1.0)
        var curveNextIdx = nextDenseIdx

        for (i in 1..walkPositions.lastIndex) {
            val segStart = walkPositions[i - 1]
            val segEnd = walkPositions[i]
            val segLen = segStart.distance(segEnd)
            val remaining = maxDist - totalDist
            if (segLen >= remaining) {
                val dir = (segEnd - segStart).normalize()
                curveEndPos = segStart + dir * remaining
                curveTangent = dir
                curveNextIdx = nextDenseIdx + i - 1
                break
            }
            totalDist += segLen
            curveEndPos = segEnd
            curveTangent = (segEnd - segStart).normalize()
            curveNextIdx = nextDenseIdx + i - 1
        }
        if (curveEndPos == null) return

        val dirToCurve = (curveEndPos - eyePos).normalize()
        val anchor = LorenzVec(eyePos.x, anchorY + ANCHOR_Y_OFFSET, eyePos.z) + dirToCurve * ANCHOR_FORWARD_DIST
        val scale = anchor.distance(curveEndPos) * CONTROL_POINT_SCALE
        val controlPoint = curveEndPos - curveTangent * scale
        val curveEndIsWater = dense[(curveNextIdx - 1).coerceAtLeast(0)].isWater
        val bezierDepth = !eyeIsWater && !curveEndIsWater
        event.draw3DBezier2(anchor, controlPoint, curveEndPos, color, NEAR_LINE_WIDTH, bezierDepth)
        if (curveNextIdx > dense.lastIndex) return

        val firstFar = dense[curveNextIdx]
        event.draw3DLine(curveEndPos, firstFar.pos, color, NEAR_LINE_WIDTH, bezierDepth && !firstFar.isWater)
        for (i in curveNextIdx until dense.lastIndex) {
            val a = dense[i]
            val b = dense[i + 1]
            event.draw3DLine(a.pos, b.pos, color, NEAR_LINE_WIDTH, !eyeIsWater && !a.isWater && !b.isWater)
        }
    }

    private fun projectOntoPath(dense: List<DensePoint>, eyePos: LorenzVec): Pair<LorenzVec, Int> {
        var bestDistSq = Double.MAX_VALUE
        var bestPos = dense[0].pos
        var bestNextIdx = 1
        for (i in 0 until dense.lastIndex) {
            val proj = eyePos.nearestPointOnLine(dense[i].pos, dense[i + 1].pos)
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
    }

    private fun subdividePositions(positions: List<LorenzVec>): List<LorenzVec> {
        if (positions.size < 2) return positions
        val result = mutableListOf<LorenzVec>()
        var prev = positions.first()
        result.add(prev)
        for (curr in positions.drop(1)) {
            val dist = prev.distance(curr)
            if (dist > SUBDIVISION_STEP) {
                val dir = (curr - prev).normalize()
                var traveled = SUBDIVISION_STEP
                while (traveled < dist) {
                    result.add(prev + dir * traveled)
                    traveled += SUBDIVISION_STEP
                }
            }
            result.add(curr)
            prev = curr
        }
        return result
    }

    private fun findClosestIndex(positions: List<DensePoint>, referencePos: LorenzVec): Int =
        positions.indices.minBy { positions[it].pos.distance(referencePos) }
}
