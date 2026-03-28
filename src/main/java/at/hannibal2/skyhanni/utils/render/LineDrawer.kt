package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.LocationUtils.calculateEdges
import at.hannibal2.skyhanni.utils.VectorUtils.edges
import at.hannibal2.skyhanni.utils.VectorUtils.inverse
import at.hannibal2.skyhanni.utils.VectorUtils.minus
import at.hannibal2.skyhanni.utils.VectorUtils.plus
import at.hannibal2.skyhanni.utils.VectorUtils.times
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.zipWithNext3
import com.mojang.blaze3d.vertex.PoseStack.Pose
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.awt.Color

class LineDrawer @PublishedApi internal constructor(val event: SkyHanniRenderWorldEvent, val lineWidth: Int, val depth: Boolean) {

    private val queuedLines = mutableListOf<QueuedLine>()

    //? if < 1.21.11
    @Suppress("UNUSED_PARAMETER")
    private fun VertexConsumer.addLineVertex(
        matrix: Pose,
        point: Vec3,
        normal: Vec3,
        color: Color,
        lineWidth: Float,
    ) {
        addVertex(matrix.pose(), point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
            .setNormal(matrix, normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
            .setColor(color.red, color.green, color.blue, color.alpha)
        /*? if > 1.21.10 {*//*.setLineWidth(lineWidth.toFloat()) *//*?}*/
    }

    @PublishedApi
    internal fun drawQueuedLines() {
        if (queuedLines.isEmpty()) return

        val layer = SkyHanniRenderLayers.getLines(lineWidth.toDouble(), !depth)
        val buf = event.vertexConsumers.getBuffer(layer)
        val matrix = event.matrices.last()

        for (line in queuedLines) {
            buf.addLineVertex(matrix, line.p1, line.normal, line.color, lineWidth.toFloat())
            buf.addLineVertex(matrix, line.p2, line.normal, line.color, lineWidth.toFloat())
        }

        queuedLines.clear()
    }

    private fun addQueuedLine(p1: Vec3, p2: Vec3, color: Color) {
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

    fun drawPath(path: List<Vec3>, color: Color, bezierPoint: Double = 1.0) {
        if (bezierPoint < 0) {
            path.zipWithNext().forEach {
                draw3DLine(it.first, it.second, color)
            }
        } else {
            val pathLines = path.zipWithNext()
            pathLines.forEachIndexed { index, pathLine ->
                val reduce = (pathLine.second - pathLine.first).normalize() * bezierPoint
                draw3DLine(
                    if (index != 0) pathLine.first + reduce else pathLine.first,
                    if (index != pathLines.lastIndex) pathLine.second - reduce else pathLine.second,
                    color,
                )
            }
            path.zipWithNext3().forEach {
                drawBezier2(
                    (it.second - (it.second - it.first).normalize()) * bezierPoint,
                    it.second,
                    (it.second - (it.second - it.third).normalize()) * bezierPoint,
                    color,
                )
            }
        }
    }

    fun drawEdges(location: Vec3, color: Color) {
        for ((p1, p2) in location.edges) {
            draw3DLine(p1, p2, color)
        }
    }

    fun drawEdges(axisAlignedBB: AABB, color: Color) {
        // TODO add cache. maybe on the caller site, since we cant add a lazy member in AxisAlignedBB
        for ((p1, p2) in axisAlignedBB.calculateEdges()) {
            draw3DLine(p1, p2, color)
        }
    }

    fun draw3DLine(p1: Vec3, p2: Vec3, color: Color) {
        addQueuedLine(p1, p2, color)
    }

    fun drawBezier2(
        p1: Vec3,
        p2: Vec3,
        p3: Vec3,
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

    private fun calculateBezierPoint(t: Float, p1: Vec3, p2: Vec3, p3: Vec3): Vec3 {
        val u = 1 - t
        val tt = t * t
        val uu = u * u

        return Vec3(
            uu * p1.x + 2 * u * t * p2.x + tt * p3.x,
            uu * p1.y + 2 * u * t * p2.y + tt * p3.y,
            uu * p1.z + 2 * u * t * p2.z + tt * p3.z,
        )
    }

    companion object {
        inline fun draw3D(
            event: SkyHanniRenderWorldEvent,
            lineWidth: Int,
            depth: Boolean,
            crossinline draws: LineDrawer.() -> Unit,
        ) {
            event.matrices.pushPose()

            val inverseView = WorldRenderUtils.getViewerPos().inverse()
            event.matrices.translate(inverseView.x, inverseView.y, inverseView.z)

            val lineDrawer = LineDrawer(event, lineWidth, depth)
            draws.invoke(lineDrawer)
            lineDrawer.drawQueuedLines()

            event.matrices.popPose()
        }
    }
}

private data class QueuedLine(
    val p1: Vec3,
    val p2: Vec3,
    val color: Color,
) {
    val normal: Vec3 = (p2 - p1).normalize()
}
