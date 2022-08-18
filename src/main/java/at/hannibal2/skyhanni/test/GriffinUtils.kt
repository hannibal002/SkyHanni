package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.client.event.RenderWorldLastEvent
import java.awt.Color

object GriffinUtils {


    fun RenderWorldLastEvent.drawWaypoint(location: LorenzVec, color: LorenzColor, beacon: Boolean = false) {
        GriffinJavaUtils.drawWaypoint(location, partialTicks, color.toColor(), beacon)
    }

    fun RenderWorldLastEvent.drawWaypointFilled(location: LorenzVec, color: Color, beacon: Boolean = false) {
        val (viewerX, viewerY, viewerZ) = RenderUtils.getViewerPos(partialTicks)
        val x = location.x - viewerX
        val y = location.y - viewerY
        val z = location.z - viewerZ
        val distSq = x * x + y * y + z * z
        GlStateManager.disableDepth()
        GlStateManager.disableCull()
        RenderUtils.drawFilledBoundingBox(
            AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1).expandBlock(),
            color,
            (0.1f + 0.005f * distSq.toFloat()).coerceAtLeast(0.2f)
        )
        GlStateManager.disableTexture2D()
        if (distSq > 5 * 5 && beacon) RenderUtils.renderBeaconBeam(x, y + 1, z, color.rgb, 1.0f, partialTicks)
        GlStateManager.disableLighting()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.enableCull()
    }

    fun RenderWorldLastEvent.draw3DLine(
        p1: LorenzVec,
        p2: LorenzVec,
        color: LorenzColor,
        lineWidth: Int,
        depth: Boolean
    ) {

        GriffinJavaUtils.draw3DLine(p1, p2, color.toColor(), lineWidth, depth, partialTicks)
    }

//    fun renderBeaconBeam(x: Double, y: Double, z: Double, rgb: Int, alphaMultiplier: Float, partialTicks: Float) {
//        val height = 300
//        val bottomOffset = 0
//        val topOffset = bottomOffset + height
//        val tessellator = Tessellator.getInstance()
//        val worldrenderer = tessellator.worldRenderer
//
////        Skytils.mc.textureManager.bindTexture(RenderUtil.beaconBeam)
//        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10497.0f)
//        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10497.0f)
//        GlStateManager.disableLighting()
//        GlStateManager.enableCull()
//        GlStateManager.enableTexture2D()
//        GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0)
//        GlStateManager.enableBlend()
//        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
//        val time = Skytils.mc.theWorld.totalWorldTime + partialTicks.toDouble()
//        val d1 = MathHelper.func_181162_h(
//            -time * 0.2 - MathHelper.floor_double(-time * 0.1)
//                .toDouble()
//        )
//        val r = (rgb shr 16 and 0xFF) / 255f
//        val g = (rgb shr 8 and 0xFF) / 255f
//        val b = (rgb and 0xFF) / 255f
//        val d2 = time * 0.025 * -1.5
//        val d4 = 0.5 + cos(d2 + 2.356194490192345) * 0.2
//        val d5 = 0.5 + sin(d2 + 2.356194490192345) * 0.2
//        val d6 = 0.5 + cos(d2 + Math.PI / 4.0) * 0.2
//        val d7 = 0.5 + sin(d2 + Math.PI / 4.0) * 0.2
//        val d8 = 0.5 + cos(d2 + 3.9269908169872414) * 0.2
//        val d9 = 0.5 + sin(d2 + 3.9269908169872414) * 0.2
//        val d10 = 0.5 + cos(d2 + 5.497787143782138) * 0.2
//        val d11 = 0.5 + sin(d2 + 5.497787143782138) * 0.2
//        val d14 = -1.0 + d1
//        val d15 = height.toDouble() * 2.5 + d14
//        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
//        worldrenderer.pos(x + d4, y + topOffset, z + d5).tex(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + d4, y + bottomOffset, z + d5).tex(1.0, d14).color(r, g, b, 1.0f).endVertex()
//        worldrenderer.pos(x + d6, y + bottomOffset, z + d7).tex(0.0, d14).color(r, g, b, 1.0f).endVertex()
//        worldrenderer.pos(x + d6, y + topOffset, z + d7).tex(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + d10, y + topOffset, z + d11).tex(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + d10, y + bottomOffset, z + d11).tex(1.0, d14).color(r, g, b, 1.0f).endVertex()
//        worldrenderer.pos(x + d8, y + bottomOffset, z + d9).tex(0.0, d14).color(r, g, b, 1.0f).endVertex()
//        worldrenderer.pos(x + d8, y + topOffset, z + d9).tex(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + d6, y + topOffset, z + d7).tex(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + d6, y + bottomOffset, z + d7).tex(1.0, d14).color(r, g, b, 1.0f).endVertex()
//        worldrenderer.pos(x + d10, y + bottomOffset, z + d11).tex(0.0, d14).color(r, g, b, 1.0f).endVertex()
//        worldrenderer.pos(x + d10, y + topOffset, z + d11).tex(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + d8, y + topOffset, z + d9).tex(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + d8, y + bottomOffset, z + d9).tex(1.0, d14).color(r, g, b, 1.0f).endVertex()
//        worldrenderer.pos(x + d4, y + bottomOffset, z + d5).tex(0.0, d14).color(r, g, b, 1.0f).endVertex()
//        worldrenderer.pos(x + d4, y + topOffset, z + d5).tex(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
//            .endVertex()
//        tessellator.draw()
//        GlStateManager.disableCull()
//        val d12 = -1.0 + d1
//        val d13 = height + d12
//        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
//        worldrenderer.pos(x + 0.2, y + topOffset, z + 0.2).tex(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + 0.2, y + bottomOffset, z + 0.2).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
//        worldrenderer.pos(x + 0.8, y + bottomOffset, z + 0.2).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
//        worldrenderer.pos(x + 0.8, y + topOffset, z + 0.2).tex(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + 0.8, y + topOffset, z + 0.8).tex(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + 0.8, y + bottomOffset, z + 0.8).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
//        worldrenderer.pos(x + 0.2, y + bottomOffset, z + 0.8).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
//        worldrenderer.pos(x + 0.2, y + topOffset, z + 0.8).tex(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + 0.8, y + topOffset, z + 0.2).tex(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + 0.8, y + bottomOffset, z + 0.2).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
//        worldrenderer.pos(x + 0.8, y + bottomOffset, z + 0.8).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
//        worldrenderer.pos(x + 0.8, y + topOffset, z + 0.8).tex(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + 0.2, y + topOffset, z + 0.8).tex(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
//            .endVertex()
//        worldrenderer.pos(x + 0.2, y + bottomOffset, z + 0.8).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
//        worldrenderer.pos(x + 0.2, y + bottomOffset, z + 0.2).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
//        worldrenderer.pos(x + 0.2, y + topOffset, z + 0.2).tex(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
//            .endVertex()
//        tessellator.draw()
//    }
}