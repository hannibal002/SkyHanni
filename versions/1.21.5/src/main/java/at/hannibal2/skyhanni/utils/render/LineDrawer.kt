package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.LocationUtils.calculateEdges
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.zipWithNext3
import net.minecraft.util.math.Box
import java.awt.Color

class LineDrawer @PublishedApi internal constructor(val event: SkyHanniRenderWorldEvent, val lineWidth: Int, val depth: Boolean) {

    private val queuedLines = mutableListOf<QueuedLine>()

    @PublishedApi
    internal fun drawQueuedLines() {
        if (queuedLines.isEmpty()) return

        val layer = SkyHanniRenderLayers.getLines(lineWidth.toDouble(), !depth)
        val buf = event.vertexConsumers.getBuffer(layer)
        val matrix = event.matrices.peek()

        for (line in queuedLines) {
            buf.vertex(matrix.positionMatrix, line.p1.x.toFloat(), line.p1.y.toFloat(), line.p1.z.toFloat())
                .normal(matrix, line.normal.x.toFloat(), line.normal.y.toFloat(), line.normal.z.toFloat())
                .color(line.color.red, line.color.green, line.color.blue, line.color.alpha)

            buf.vertex(matrix.positionMatrix, line.p2.x.toFloat(), line.p2.y.toFloat(), line.p2.z.toFloat())
                .normal(matrix, line.normal.x.toFloat(), line.normal.y.toFloat(), line.normal.z.toFloat())
                .color(line.color.red, line.color.green, line.color.blue, line.color.alpha)
        }

        queuedLines.clear()
    }

    private fun addQueuedLine(p1: LorenzVec, p2: LorenzVec, color: Color) {
        val last = queuedLines.lastOrNull()

        if (last == null) {
            queuedLines.add(QueuedLine(p1, p2, color))
            return
        }

        if (last.p2 != p1) {
            drawQueuedLines()
        }

        queuedLines.add(QueuedLine(p1, p2, color))
    }

    fun drawPath(path: List<LorenzVec>, color: Color, bezierPoint: Double = 1.0) {
        if (bezierPoint < 0) {
            path.zipWithNext().forEach {
                draw3DLine(it.first, it.second, color)
            }
        } else {
            val pathLines = path.zipWithNext()
            pathLines.forEachIndexed { index, pathLine ->
                val reduce = pathLine.second.minus(pathLine.first).normalize().times(bezierPoint)
                draw3DLine(
                    if (index != 0) pathLine.first + reduce else pathLine.first,
                    if (index != pathLines.lastIndex) pathLine.second - reduce else pathLine.second,
                    color,
                )
            }
            path.zipWithNext3().forEach {
                val p1 = it.second.minus(it.second.minus(it.first).normalize().times(bezierPoint))
                val p3 = it.second.minus(it.second.minus(it.third).normalize().times(bezierPoint))
                val p2 = it.second
                drawBezier2(p1, p2, p3, color)
            }
        }
    }

    fun drawEdges(location: LorenzVec, color: Color) {
        for ((p1, p2) in location.edges) {
            draw3DLine(p1, p2, color)
        }
    }

    fun drawEdges(axisAlignedBB: Box, color: Color) {
        // TODO add cache. maybe on the caller site, since we cant add a lazy member in AxisAlignedBB
        for ((p1, p2) in axisAlignedBB.calculateEdges()) {
            draw3DLine(p1, p2, color)
        }
    }

    fun draw3DLine(p1: LorenzVec, p2: LorenzVec, color: Color) {
        addQueuedLine(p1, p2, color)
    }

    fun drawBezier2(
        p1: LorenzVec,
        p2: LorenzVec,
        p3: LorenzVec,
        color: Color,
        segments: Int = 30,
    ) {
        for (i in 0 until segments) {
            val t1 = i.toFloat() / segments
            val t2 = (i + 1).toFloat() / segments

            val point1 = calculateBezierPoint(t1, p1, p2, p3)
            val point2 = calculateBezierPoint(t2, p1, p2, p3)

            addQueuedLine(point1, point2, color)
        }
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
            lineWidth: Int,
            depth: Boolean,
            crossinline draws: LineDrawer.() -> Unit,
        ) {
            event.matrices.push()

            val inverseView = WorldRenderUtils.getViewerPos().negated()
            event.matrices.translate(inverseView.x, inverseView.y, inverseView.z)

            val lineDrawer = LineDrawer(event, lineWidth, depth)
            draws.invoke(lineDrawer)
            lineDrawer.drawQueuedLines()

            event.matrices.pop()
        }
    }
}

private data class QueuedLine(
    val p1: LorenzVec,
    val p2: LorenzVec,
    val color: Color,
) {
    val normal = p2.minus(p1).normalize()
}
