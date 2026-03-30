package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.VectorUtils.minus
import at.hannibal2.skyhanni.utils.VectorUtils.plus
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.world.phys.Vec3
import java.awt.Color

class QuadDrawer @PublishedApi internal constructor(val event: SkyHanniRenderWorldEvent) {

    internal fun VertexConsumer.addColoredVertex(point: Vec3, color: Color) {
        addVertex(point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
            .setColor(color.red, color.green, color.blue, color.alpha)
    }

    fun draw(
        middlePoint: Vec3,
        sidePoint1: Vec3,
        sidePoint2: Vec3,
        color: Color,
    ) {
        val layer = SkyHanniRenderLayers.getQuads(false)
        val buf = event.vertexConsumers.getBuffer(layer)
        event.matrices.pushPose()

        val viewerPos = WorldRenderUtils.getViewerPos()
        val newMidPoint = middlePoint - viewerPos
        val newSidePoint1 = sidePoint1 - viewerPos
        val newSidePoint2 = sidePoint2 - viewerPos
        val lastPoint = sidePoint1 + sidePoint2 - middlePoint
        val newLastPoint = lastPoint - viewerPos

        buf.addColoredVertex(newSidePoint1, color)
        buf.addColoredVertex(newMidPoint, color)
        buf.addColoredVertex(newSidePoint2, color)
        buf.addColoredVertex(newLastPoint, color)

        event.matrices.popPose()
    }

    companion object {
        inline fun draw3D(
            event: SkyHanniRenderWorldEvent,
            crossinline quads: QuadDrawer.() -> Unit,
        ) {
            quads.invoke(QuadDrawer(event))
        }
    }
}
