package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.LocationUtils.calculateEdges
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.zipWithNext3
import net.minecraft.util.math.Box
import java.awt.Color

class LineDrawer @PublishedApi internal constructor(val event: SkyHanniRenderWorldEvent) {

    fun drawPath(path: List<LorenzVec>, color: Color, lineWidth: Int, depth: Boolean, bezierPoint: Double = 1.0) {
        if (bezierPoint < 0) {
            path.zipWithNext().forEach {
                draw3DLine(it.first, it.second, color, lineWidth, depth)
            }
        } else {
            val pathLines = path.zipWithNext()
            pathLines.forEachIndexed { index, pathLine ->
                val reduce = pathLine.second.minus(pathLine.first).normalize().times(bezierPoint)
                draw3DLine(
                    if (index != 0) pathLine.first + reduce else pathLine.first,
                    if (index != pathLines.lastIndex) pathLine.second - reduce else pathLine.second,
                    color,
                    lineWidth,
                    depth,
                )
            }
            path.zipWithNext3().forEach {
                val p1 = it.second.minus(it.second.minus(it.first).normalize().times(bezierPoint))
                val p3 = it.second.minus(it.second.minus(it.third).normalize().times(bezierPoint))
                val p2 = it.second
                drawBezier2(p1, p2, p3, color, lineWidth, depth)
            }
        }
    }

    fun drawEdges(location: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        for ((p1, p2) in location.edges) {
            draw3DLine(p1, p2, color, lineWidth, depth)
        }
    }

    fun drawEdges(axisAlignedBB: Box, color: Color, lineWidth: Int, depth: Boolean) {
        // TODO add cache. maybe on the caller site, since we cant add a lazy member in AxisAlignedBB
        for ((p1, p2) in axisAlignedBB.calculateEdges()) {
            draw3DLine(p1, p2, color, lineWidth, depth)
        }
    }

    fun draw3DLine(p1: LorenzVec, p2: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        val layer = SkyHanniRenderLayers.getLines(lineWidth.toDouble(), !depth)
        val buf = SkyHanniRenderLayers.getBufferFromLayer(layer)
        val matrix = event.matrices.peek()
        val normal = p2.minus(p1).normalize()

        buf.vertex(matrix.positionMatrix, p1.x.toFloat(), p1.y.toFloat(), p1.z.toFloat())
            .normal(matrix, normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
            .color(color.red, color.green, color.blue, color.alpha)

        buf.vertex(matrix.positionMatrix, p2.x.toFloat(), p2.y.toFloat(), p2.z.toFloat())
            .normal(matrix, normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
            .color(color.red, color.green, color.blue, color.alpha)

        layer.draw(buf.end())
    }

    fun drawBezier2(
        p1: LorenzVec,
        p2: LorenzVec,
        p3: LorenzVec,
        color: Color,
        lineWidth: Int,
        depth: Boolean,
        segments: Int = 30,
    ) {
        val layer = SkyHanniRenderLayers.getLines(lineWidth.toDouble(), !depth)
        val buf = SkyHanniRenderLayers.getBufferFromLayer(layer)
        val matrix = event.matrices.peek()

        for (i in 0 until segments) {
            val t1 = i.toFloat() / segments
            val t2 = (i + 1).toFloat() / segments

            val point1 = calculateBezierPoint(t1, p1, p2, p3)
            val point2 = calculateBezierPoint(t2, p1, p2, p3)
            val normal = point2.minus(point1).normalize()

            buf.vertex(matrix.positionMatrix, point1.x.toFloat(), point1.y.toFloat(), point1.z.toFloat())
                .normal(matrix, normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
                .color(color.red, color.green, color.blue, color.alpha)

            buf.vertex(matrix.positionMatrix, point2.x.toFloat(), point2.y.toFloat(), point2.z.toFloat())
                .normal(matrix, normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
                .color(color.red, color.green, color.blue, color.alpha)
        }

        layer.draw(buf.end())
    }

    private fun calculateBezierPoint(t: Float, p1: LorenzVec, p2: LorenzVec, p3: LorenzVec): LorenzVec {
        val u = 1 - t
        val tt = t * t
        val uu = u * u

        val x = uu * p1.x + 2 * u * t * p2.x + tt * p3.x
        val y = uu * p1.y + 2 * u * t * p2.y + tt * p3.y
        val z = uu * p1.z + 2 * u * t * p2.z + tt * p3.z

        return LorenzVec(x, y, z)
    }

    companion object {
        inline fun draw3D(
            event: SkyHanniRenderWorldEvent,
            crossinline draws: LineDrawer.() -> Unit,
        ) {
            event.matrices.push()

            val inverseView = WorldRenderUtils.getViewerPos().negated()
            event.matrices.translate(inverseView.x, inverseView.y, inverseView.z)

            draws.invoke(LineDrawer(event))

            event.matrices.pop()
        }
    }
}
