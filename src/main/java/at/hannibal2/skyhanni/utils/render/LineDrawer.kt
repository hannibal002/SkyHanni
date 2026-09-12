package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.LocationUtils.calculateEdges
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.zipWithNext3
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.submitCustomGeometry
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.world.phys.AABB
import org.joml.Vector3f
import java.awt.Color

class LineDrawer @PublishedApi internal constructor(
    val event: SkyHanniRenderWorldEvent,
    val lineWidth: Int,
    val depth: Boolean,
) {
    private val queuedLines = mutableListOf<QueuedLine>()
    private val cameraPos = event.camera.pos.toLorenzVec()
    private val cameraLook = Vector3f(0f, 0f, -1f).rotate(event.camera.orientation).let {
        LorenzVec(it.x.toDouble(), it.y.toDouble(), it.z.toDouble())
    }

    @PublishedApi
    internal fun drawQueuedLines() {
        if (queuedLines.isEmpty()) return

        val layer = SkyHanniRenderLayers.getLines(!depth)
        val lines = queuedLines.mapNotNull { it.clipToCamera() }
        event.submitCustomGeometry(layer) { pose, buf ->
            fun QueuedLine.addVertexForPoint(point: LorenzVec) {
                buf.addVertex(pose.pose(), point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
                    .setNormal(pose, normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
                    .setColor(color.red, color.green, color.blue, color.alpha)
                    .setLineWidth(lineWidth.toFloat())
            }

            for (line in lines) {
                line.addVertexForPoint(line.p1)
                line.addVertexForPoint(line.p2)
            }
        }

        queuedLines.clear()
    }

    // The line shader expands each vertex into a quad edge that folds over itself once the vertex is behind the
    // camera, and which of the two folded shapes gets rasterized depends on gl_VertexID parity. Since 26.2 all
    // world geometry shares one vertex buffer with a per-frame base vertex, so that parity flickers between frames.
    private fun QueuedLine.clipToCamera(): QueuedLine? {
        val depth1 = (p1 - cameraPos).dotProduct(cameraLook)
        val depth2 = (p2 - cameraPos).dotProduct(cameraLook)
        if (depth1 >= MIN_CAMERA_DEPTH && depth2 >= MIN_CAMERA_DEPTH) return this
        if (depth1 < MIN_CAMERA_DEPTH && depth2 < MIN_CAMERA_DEPTH) return null
        val cut = p1 + (p2 - p1) * ((MIN_CAMERA_DEPTH - depth1) / (depth2 - depth1))
        return if (depth1 < MIN_CAMERA_DEPTH) copy(p1 = cut) else copy(p2 = cut)
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

    fun drawEdges(axisAlignedBB: AABB, color: Color) {
        // TODO add cache. maybe on the caller site, since we can't add a lazy member in AxisAlignedBB
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
        // Slightly in front of the near plane (0.05) so no vertex ends up at w = 0
        private const val MIN_CAMERA_DEPTH = 0.1

        inline fun draw3D(
            event: SkyHanniRenderWorldEvent,
            lineWidth: Int,
            depth: Boolean,
            crossinline draws: LineDrawer.() -> Unit,
        ) {
            event.matrices.pushPose()

            val inverseView = WorldRenderUtils.getViewerPos().negated()
            event.matrices.translate(inverseView.x, inverseView.y, inverseView.z)

            val lineDrawer = LineDrawer(event, lineWidth, depth)
            draws.invoke(lineDrawer)
            lineDrawer.drawQueuedLines()

            event.matrices.popPose()
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
