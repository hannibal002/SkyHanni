package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.pos
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11
import java.awt.Color

class QuadDrawer @PublishedApi internal constructor(val tessellator: Tessellator) {

    val worldRenderer = tessellator.worldRenderer

    inline fun draw(
        middlePoint: LorenzVec,
        sidePoint1: LorenzVec,
        sidePoint2: LorenzVec,
        c: Color,
    ) {
        GlStateManager.color(c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f)
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(sidePoint1).endVertex()
        worldRenderer.pos(middlePoint).endVertex()
        worldRenderer.pos(sidePoint2).endVertex()
        worldRenderer.pos(sidePoint1 + sidePoint2 - middlePoint).endVertex()
        tessellator.draw()
    }

    companion object {
        inline fun draw3D(
            event: SkyHanniRenderWorldEvent,
            crossinline quads: QuadDrawer.() -> Unit,
        ) {
            GlStateManager.enableBlend()
            GlStateManager.disableLighting()
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
            GlStateManager.disableTexture2D()
            GlStateManager.disableCull()

            GlStateManager.pushMatrix()
            WorldRenderUtils.translate(WorldRenderUtils.getViewerPos(event.partialTicks).negated())

            quads.invoke(QuadDrawer(Tessellator.getInstance()))

            GlStateManager.popMatrix()

            GlStateManager.enableTexture2D()
            GlStateManager.enableCull()
            GlStateManager.disableBlend()
        }
    }
}
