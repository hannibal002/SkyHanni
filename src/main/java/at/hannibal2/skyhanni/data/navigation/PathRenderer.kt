package at.hannibal2.skyhanni.data.navigation

import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.GraphUtils.playerPosition
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DBezier2
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DPolyline
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import java.awt.Color

/**
 * TODO
 *
 * bug: hides the two separate render things, if one is just out of line, instead, should the static one always show,
 *  and the near one should "Jump" closer to me on the line until it finds the correct one finally
 * bug: not through water line
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
 */

private const val SUBDIVISION_STEP = 0.5
private const val CURVE_RADIUS = 8.0

private const val ANCHOR_Y_OFFSET = -1.0
private const val ANCHOR_FORWARD_DIST = 0.7

private const val CONTROL_POINT_SCALE = 0.5

private const val NEAR_LINE_WIDTH = 6
private const val FAR_LINE_WIDTH = 4

private const val STANDING_EYE_HEIGHT = 1.62

/**
 * Uses tick and render events to calculate the final pathfind lines.
 */
class PathRenderer(val path: Graph, private val color: Color, private val targetLocation: LorenzVec) {

    private val densePositions: List<LorenzVec> = subdividePositions(path.map { it.position.addHalf() })
    private var curveMaxDist: Double = 0.0

    fun render(event: SkyHanniRenderWorldEvent) {
        renderCurve(event)
        val lastNode = path.lastOrNull()?.position ?: return
        event.draw3DLine(lastNode.addHalf(), targetLocation.addHalf(), color, FAR_LINE_WIDTH, true)
    }

    private fun renderCurve(event: SkyHanniRenderWorldEvent) {
        val eyePos = event.exactPlayerEyeLocation()
        val anchorY = eyePos.y - MinecraftCompat.localPlayer.eyeHeight + STANDING_EYE_HEIGHT
        val dense = densePositions
        val maxDist = curveMaxDist
        if (dense.size < 2 || maxDist <= 0.0) return

        val (startPos, nextDenseIdx) = projectOntoPath(dense, eyePos)
        val walkPositions = listOf(startPos) + dense.drop(nextDenseIdx)
        var totalDist = 0.0
        var curveEndPos: LorenzVec? = null
        var curveTangent = LorenzVec(0.0, 0.0, 1.0)
        var curveNextIdx = nextDenseIdx
        for (i in 1..walkPositions.lastIndex) {
            val segLen = walkPositions[i - 1].distance(walkPositions[i])
            val remaining = maxDist - totalDist
            if (segLen >= remaining) {
                val dir = (walkPositions[i] - walkPositions[i - 1]).normalize()
                curveEndPos = walkPositions[i - 1] + dir * remaining
                curveTangent = dir
                curveNextIdx = nextDenseIdx + i - 1
                break
            }
            totalDist += segLen
            curveEndPos = walkPositions[i]
            curveTangent = (walkPositions[i] - walkPositions[i - 1]).normalize()
            curveNextIdx = nextDenseIdx + i - 1
        }

        if (curveEndPos == null) return
        val dirToCurve = (curveEndPos - eyePos).normalize()
        val anchor = LorenzVec(eyePos.x, anchorY + ANCHOR_Y_OFFSET, eyePos.z) + dirToCurve * ANCHOR_FORWARD_DIST
        val scale = anchor.distance(curveEndPos) * CONTROL_POINT_SCALE
        val controlPoint = curveEndPos - curveTangent * scale
        event.draw3DBezier2(anchor, controlPoint, curveEndPos, color, NEAR_LINE_WIDTH, true)
        val farPositions = listOf(curveEndPos) + dense.drop(curveNextIdx)
        if (farPositions.size >= 2) event.draw3DPolyline(farPositions, color, NEAR_LINE_WIDTH, true)
    }


    private fun projectOntoPath(dense: List<LorenzVec>, eyePos: LorenzVec): Pair<LorenzVec, Int> {
        var bestDistSq = Double.MAX_VALUE
        var bestPos = dense[0]
        var bestNextIdx = 1
        for (i in 0 until dense.lastIndex) {
            val proj = eyePos.nearestPointOnLine(dense[i], dense[i + 1])
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
        val dense = densePositions
        val closestIdx = findClosestIndex(dense, playerPosition)
        var totalDist = 0.0
        for (i in (closestIdx + 1)..dense.lastIndex) {
            if (!dense[i].canBeSeen()) break
            totalDist += dense[i - 1].distance(dense[i])
            if (totalDist >= CURVE_RADIUS) {
                totalDist = CURVE_RADIUS; break
            }
        }
        curveMaxDist = totalDist
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

    private fun findClosestIndex(positions: List<LorenzVec>, referencePos: LorenzVec): Int =
        positions.indices.minBy { positions[it].distance(referencePos) }
}
