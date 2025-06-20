package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.LocationUtils.calculateEdges
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.zipWithNext3
//#if TODO
import net.minecraft.client.renderer.GLAllocation
//#endif
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.AxisAlignedBB
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.nio.FloatBuffer

class LineDrawer @PublishedApi internal constructor(val tessellator: Tessellator, val inverseView: LorenzVec) {

    //#if TODO
    val worldRenderer = tessellator.worldRenderer
    //#endif

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

    fun drawEdges(axisAlignedBB: AxisAlignedBB, color: Color, lineWidth: Int, depth: Boolean) {
        // TODO add cache. maybe on the caller site, since we cant add a lazy member in AxisAlignedBB
        for ((p1, p2) in axisAlignedBB.calculateEdges()) {
            draw3DLine(p1, p2, color, lineWidth, depth)
        }
    }

    fun draw3DLine(p1: LorenzVec, p2: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        //#if TODO
        GL11.glLineWidth(lineWidth.toFloat())
        if (!depth) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GlStateManager.depthMask(false)
        }
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        worldRenderer.pos(p1.x, p1.y, p1.z).endVertex()
        worldRenderer.pos(p2.x, p2.y, p2.z).endVertex()
        tessellator.draw()
        if (!depth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GlStateManager.depthMask(true)
        }
        //#endif
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
        //#if TODO
        GL11.glLineWidth(lineWidth.toFloat())
        if (!depth) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GlStateManager.depthMask(false)
        }
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        val ctrlPoints = p1.toFloatArray() + p2.toFloatArray() + p3.toFloatArray()
        bezier2Buffer.clear()
        ctrlPoints.forEach {
            bezier2Buffer.put(it)
        }
        bezier2Buffer.flip()
        GL11.glMap1f(
            GL11.GL_MAP1_VERTEX_3,
            0f,
            1f,
            3,
            3,
            bezier2Buffer,
        )

        GL11.glEnable(GL11.GL_MAP1_VERTEX_3)

        GL11.glBegin(GL11.GL_LINE_STRIP)
        for (i in 0..segments) {
            GL11.glEvalCoord1f(i.toFloat() / segments.toFloat())
        }
        GL11.glEnd()
        if (!depth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GlStateManager.depthMask(true)
        }
        //#endif
    }

    companion object {
        inline fun draw3D(
            partialTicks: Float = 0F,
            crossinline draws: LineDrawer.() -> Unit,
        ) {
            //#if TODO
            GlStateManager.enableBlend()
            GlStateManager.disableLighting()
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
            GlStateManager.disableTexture2D()
            GlStateManager.disableCull()
            GlStateManager.disableAlpha()

            GlStateManager.pushMatrix()
            val inverseView = RenderUtils.getViewerPos(partialTicks)
            WorldRenderUtils.translate(inverseView.negated())

            draws.invoke(LineDrawer(Tessellator.getInstance(), inverseView))

            GlStateManager.popMatrix()

            GlStateManager.enableAlpha()
            GlStateManager.enableTexture2D()
            GlStateManager.enableCull()
            GlStateManager.disableBlend()
            GlStateManager.color(1f, 1f, 1f, 1f)
            //#endif
        }

        //#if TODO
        private val bezier2Buffer: FloatBuffer = GLAllocation.createDirectFloatBuffer(9)
        //#endif
    }
}
