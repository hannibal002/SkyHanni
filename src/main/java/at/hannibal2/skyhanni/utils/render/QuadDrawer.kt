package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.LorenzVec
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.rendertype.RenderType
import java.awt.Color

class QuadDrawer @PublishedApi internal constructor(val event: SkyHanniRenderWorldEvent) {

    private fun SkyHanniRenderWorldEvent.submitCustomGeometry(layer: RenderType, block: (VertexConsumer) -> Unit) {
        //? if >= 26.2 {
        event.submitNodeStorage.submitCustomGeometry(event.matrices, layer) { _, buf -> block(buf) }
        //?} else
        //block(event.bufferSource.getBuffer(layer))
    }

    fun draw(
        middlePoint: LorenzVec,
        sidePoint1: LorenzVec,
        sidePoint2: LorenzVec,
        c: Color,
    ) {
        event.matrices.pushPose()
        val layer = SkyHanniRenderLayers.getQuads(throughWalls = false)

        event.submitCustomGeometry(layer) { buf ->
            val viewerPos = WorldRenderUtils.getViewerPos()
            val newMidPoint = middlePoint - viewerPos
            val newSidePoint1 = sidePoint1 - viewerPos
            val newSidePoint2 = sidePoint2 - viewerPos
            val lastPoint = sidePoint1 + sidePoint2 - middlePoint
            val newLastPoint = lastPoint - viewerPos

            buf.addVertex(newSidePoint1.x.toFloat(), newSidePoint1.y.toFloat(), newSidePoint1.z.toFloat())
                .setColor(c.red, c.green, c.blue, c.alpha)
            buf.addVertex(newMidPoint.x.toFloat(), newMidPoint.y.toFloat(), newMidPoint.z.toFloat())
                .setColor(c.red, c.green, c.blue, c.alpha)
            buf.addVertex(newSidePoint2.x.toFloat(), newSidePoint2.y.toFloat(), newSidePoint2.z.toFloat())
                .setColor(c.red, c.green, c.blue, c.alpha)
            buf.addVertex(newLastPoint.x.toFloat(), newLastPoint.y.toFloat(), newLastPoint.z.toFloat())
                .setColor(c.red, c.green, c.blue, c.alpha)
        }

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
