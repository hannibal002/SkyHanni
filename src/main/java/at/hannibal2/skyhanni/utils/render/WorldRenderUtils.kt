package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.ColorUtils.getFirstColorCode
import at.hannibal2.skyhanni.utils.ColorUtils.rgb
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils.getCornersAtHeight
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.createResourceLocation
import at.hannibal2.skyhanni.utils.expand
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MathHelper
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("FunctionNaming")
object WorldRenderUtils {

    private val beaconBeam = createResourceLocation("textures/entity/beacon_beam.png")

    /**
     * Taken from NotEnoughUpdates under Creative Commons Attribution-NonCommercial 3.0
     * https://github.com/Moulberry/NotEnoughUpdates/blob/master/LICENSE
     * @author Moulberry
     * @author Mojang
     */
    fun SkyHanniRenderWorldEvent.renderBeaconBeam(vec: LorenzVec, rgb: Int) {
        this.renderBeaconBeam(vec.x, vec.y, vec.z, rgb)
    }

    fun SkyHanniRenderWorldEvent.renderBeaconBeam(
        x: Double,
        y: Double,
        z: Double,
        rgb: Int,
    ) {
        val height = 300
        val bottomOffset = 0
        val topOffset = bottomOffset + height
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        Minecraft.getMinecraft().textureManager.bindTexture(beaconBeam)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10497f)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10497f)
        GlStateManager.disableLighting()
        GlStateManager.enableCull()
        GlStateManager.enableTexture2D()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, 1, 1, 0)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        val time = MinecraftCompat.localWorld.totalWorldTime + partialTicks.toDouble()
        val d1 = MathHelper.func_181162_h(
            -time * 0.2 - MathHelper.floor_double(-time * 0.1)
                .toDouble(),
        )
        val r = (rgb shr 16 and 0xFF) / 255f
        val g = (rgb shr 8 and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val d2 = time * 0.025 * -1.5
        val d4 = 0.5 + cos(d2 + 2.356194490192345) * 0.2
        val d5 = 0.5 + sin(d2 + 2.356194490192345) * 0.2
        val d6 = 0.5 + cos(d2 + Math.PI / 4.0) * 0.2
        val d7 = 0.5 + sin(d2 + Math.PI / 4.0) * 0.2
        val d8 = 0.5 + cos(d2 + 3.9269908169872414) * 0.2
        val d9 = 0.5 + sin(d2 + 3.9269908169872414) * 0.2
        val d10 = 0.5 + cos(d2 + 5.497787143782138) * 0.2
        val d11 = 0.5 + sin(d2 + 5.497787143782138) * 0.2
        val d14 = -1.0 + d1
        val d15 = height.toDouble() * 2.5 + d14
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
        worldRenderer.pos(x + d4, y + topOffset, z + d5).tex(1.0, d15).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d4, y + bottomOffset, z + d5).tex(1.0, d14).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d6, y + bottomOffset, z + d7).tex(0.0, d14).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d6, y + topOffset, z + d7).tex(0.0, d15).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d10, y + topOffset, z + d11).tex(1.0, d15).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d10, y + bottomOffset, z + d11).tex(1.0, d14).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d8, y + bottomOffset, z + d9).tex(0.0, d14).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d8, y + topOffset, z + d9).tex(0.0, d15).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d6, y + topOffset, z + d7).tex(1.0, d15).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d6, y + bottomOffset, z + d7).tex(1.0, d14).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d10, y + bottomOffset, z + d11).tex(0.0, d14).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d10, y + topOffset, z + d11).tex(0.0, d15).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d8, y + topOffset, z + d9).tex(1.0, d15).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d8, y + bottomOffset, z + d9).tex(1.0, d14).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d4, y + bottomOffset, z + d5).tex(0.0, d14).color(r, g, b, 1f).endVertex()
        worldRenderer.pos(x + d4, y + topOffset, z + d5).tex(0.0, d15).color(r, g, b, 1f).endVertex()
        tessellator.draw()
        GlStateManager.disableCull()
        val d12 = -1.0 + d1
        val d13 = height + d12
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
        worldRenderer.pos(x + 0.2, y + topOffset, z + 0.2).tex(1.0, d13).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.2, y + bottomOffset, z + 0.2).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.8, y + bottomOffset, z + 0.2).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.8, y + topOffset, z + 0.2).tex(0.0, d13).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.8, y + topOffset, z + 0.8).tex(1.0, d13).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.8, y + bottomOffset, z + 0.8).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.2, y + bottomOffset, z + 0.8).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.2, y + topOffset, z + 0.8).tex(0.0, d13).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.8, y + topOffset, z + 0.2).tex(1.0, d13).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.8, y + bottomOffset, z + 0.2).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.8, y + bottomOffset, z + 0.8).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.8, y + topOffset, z + 0.8).tex(0.0, d13).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.2, y + topOffset, z + 0.8).tex(1.0, d13).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.2, y + bottomOffset, z + 0.8).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.2, y + bottomOffset, z + 0.2).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
        worldRenderer.pos(x + 0.2, y + topOffset, z + 0.2).tex(0.0, d13).color(r, g, b, 0.25f).endVertex()
        tessellator.draw()
    }

    fun SkyHanniRenderWorldEvent.drawColor(
        location: LorenzVec,
        color: ChromaColour,
        beacon: Boolean = false,
        alpha: Float = -1f,
        seeThroughBlocks: Boolean = true,
    ) {
        val (viewerX, viewerY, viewerZ) = getViewerPos(partialTicks)
        val x = location.x - viewerX
        val y = location.y - viewerY
        val z = location.z - viewerZ
        val distSq = x * x + y * y + z * z
        val realAlpha = if (alpha == -1f) {
            (0.1f + 0.005f * distSq.toFloat()).coerceAtLeast(0.2f)
        } else alpha
        if (seeThroughBlocks) {
            GlStateManager.disableDepth()
        }
        GlStateManager.disableCull()
        drawFilledBoundingBox(
            AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1).expandBlock(),
            color,
            realAlpha,
            true,
        )
        GlStateManager.disableTexture2D()
        if (distSq > 5 * 5 && beacon) renderBeaconBeam(x, y + 1, z, color.rgb)
        GlStateManager.disableLighting()
        GlStateManager.enableTexture2D()
        if (seeThroughBlocks) {
            GlStateManager.enableDepth()
        }
        GlStateManager.enableCull()
    }

    // TODO add chroma support
    fun SkyHanniRenderWorldEvent.drawWaypointFilled(
        location: LorenzVec,
        color: Color,
        seeThroughBlocks: Boolean = false,
        beacon: Boolean = false,
        extraSize: Double = 0.0,
        extraSizeTopY: Double = extraSize,
        extraSizeBottomY: Double = extraSize,
        minimumAlpha: Float = 0.2f,
        inverseAlphaScale: Boolean = false,
    ) {
        val (viewerX, viewerY, viewerZ) = getViewerPos(partialTicks)
        val x = location.x - viewerX
        val y = location.y - viewerY
        val z = location.z - viewerZ
        val distSq = x * x + y * y + z * z

        if (seeThroughBlocks) {
            GlStateManager.disableDepth()
        }

        GlStateManager.disableCull()
        drawFilledBoundingBox(
            AxisAlignedBB(
                x - extraSize, y - extraSizeBottomY, z - extraSize,
                x + 1 + extraSize, y + 1 + extraSizeTopY, z + 1 + extraSize,
            ).expandBlock(),
            color,
            if (inverseAlphaScale) (1f - 0.005f * distSq.toFloat()).coerceAtLeast(minimumAlpha)
            else (0.1f + 0.005f * distSq.toFloat()).coerceAtLeast(minimumAlpha),
            renderRelativeToCamera = true,
        )
        GlStateManager.disableTexture2D()
        if (distSq > 5 * 5 && beacon) renderBeaconBeam(x, y + 1, z, color.rgb)
        GlStateManager.disableLighting()
        GlStateManager.enableTexture2D()
        GlStateManager.enableCull()

        if (seeThroughBlocks) {
            GlStateManager.enableDepth()
        }
    }

    fun SkyHanniRenderWorldEvent.drawFilledBoundingBox(
        aabb: AxisAlignedBB,
        c: ChromaColour,
        alphaMultiplier: Float = 1f,
        /**
         * If set to `true`, renders the box relative to the camera instead of relative to the world.
         * If set to `false`, will be relativized to [WorldRenderUtils.getViewerPos].
         */
        renderRelativeToCamera: Boolean = false,
        drawVerticalBarriers: Boolean = true,
    ) {
        drawFilledBoundingBox(aabb, c.toColor(), alphaMultiplier, renderRelativeToCamera, drawVerticalBarriers)
    }

    // TODO make deprecated
    fun SkyHanniRenderWorldEvent.drawFilledBoundingBox(
        aabb: AxisAlignedBB,
        c: Color,
        alphaMultiplier: Float = 1f,
        /**
         * If set to `true`, renders the box relative to the camera instead of relative to the world.
         * If set to `false`, will be relativized to [WorldRenderUtils.getViewerPos].
         */
        renderRelativeToCamera: Boolean = false,
        drawVerticalBarriers: Boolean = true,
    ) {
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.disableTexture2D()
        GlStateManager.disableCull()
        val effectiveAABB = if (!renderRelativeToCamera) {
            val vp = getViewerPos(partialTicks)
            AxisAlignedBB(
                aabb.minX - vp.x, aabb.minY - vp.y, aabb.minZ - vp.z,
                aabb.maxX - vp.x, aabb.maxY - vp.y, aabb.maxZ - vp.z,
            )
        } else {
            aabb
        }
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        // vertical
        if (drawVerticalBarriers) {
            GlStateManager.color(c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f * alphaMultiplier)
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            with(effectiveAABB) {
                worldRenderer.pos(minX, minY, minZ).endVertex()
                worldRenderer.pos(maxX, minY, minZ).endVertex()
                worldRenderer.pos(maxX, minY, maxZ).endVertex()
                worldRenderer.pos(minX, minY, maxZ).endVertex()
                tessellator.draw()
                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
                worldRenderer.pos(minX, maxY, maxZ).endVertex()
                worldRenderer.pos(maxX, maxY, maxZ).endVertex()
                worldRenderer.pos(maxX, maxY, minZ).endVertex()
                worldRenderer.pos(minX, maxY, minZ).endVertex()
                tessellator.draw()
            }
        }
        GlStateManager.color(
            c.red / 255f * 0.8f,
            c.green / 255f * 0.8f,
            c.blue / 255f * 0.8f,
            c.alpha / 255f * alphaMultiplier,
        )

        // x
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        with(effectiveAABB) {
            worldRenderer.pos(minX, minY, maxZ).endVertex()
            worldRenderer.pos(minX, maxY, maxZ).endVertex()
            worldRenderer.pos(minX, maxY, minZ).endVertex()
            worldRenderer.pos(minX, minY, minZ).endVertex()
            tessellator.draw()
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            worldRenderer.pos(maxX, minY, minZ).endVertex()
            worldRenderer.pos(maxX, maxY, minZ).endVertex()
            worldRenderer.pos(maxX, maxY, maxZ).endVertex()
            worldRenderer.pos(maxX, minY, maxZ).endVertex()
        }
        tessellator.draw()
        GlStateManager.color(
            c.red / 255f * 0.9f,
            c.green / 255f * 0.9f,
            c.blue / 255f * 0.9f,
            c.alpha / 255f * alphaMultiplier,
        )
        // z
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        with(effectiveAABB) {
            worldRenderer.pos(minX, maxY, minZ).endVertex()
            worldRenderer.pos(maxX, maxY, minZ).endVertex()
            worldRenderer.pos(maxX, minY, minZ).endVertex()
            worldRenderer.pos(minX, minY, minZ).endVertex()
            tessellator.draw()
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            worldRenderer.pos(minX, minY, maxZ).endVertex()
            worldRenderer.pos(maxX, minY, maxZ).endVertex()
            worldRenderer.pos(maxX, maxY, maxZ).endVertex()
            worldRenderer.pos(minX, maxY, maxZ).endVertex()
        }
        tessellator.draw()
        GlStateManager.enableTexture2D()
        GlStateManager.enableCull()
        GlStateManager.disableBlend()
    }

    fun SkyHanniRenderWorldEvent.drawString(
        location: LorenzVec,
        text: String,
        seeThroughBlocks: Boolean = false,
        color: Color? = null,
    ) {
        val viewer = Minecraft.getMinecraft().renderViewEntity ?: return
        GlStateManager.alphaFunc(516, 0.1f)
        GlStateManager.pushMatrix()
        val renderManager = Minecraft.getMinecraft().renderManager
        var x = location.x - renderManager.viewerPosX
        var y = location.y - renderManager.viewerPosY - viewer.eyeHeight
        var z = location.z - renderManager.viewerPosZ
        val distSq = x * x + y * y + z * z
        val dist = sqrt(distSq)
        if (distSq > 144) {
            x *= 12 / dist
            y *= 12 / dist
            z *= 12 / dist
        }

        if (seeThroughBlocks) {
            GlStateManager.disableDepth()
            GlStateManager.disableCull()
        }

        GlStateManager.translate(x, y, z)
        GlStateManager.translate(0f, viewer.eyeHeight, 0f)
        drawNametag(text, color)
        GlStateManager.rotate(-renderManager.playerViewY, 0f, 1f, 0f)
        GlStateManager.rotate(renderManager.playerViewX, 1f, 0f, 0f)
        GlStateManager.translate(0f, -0.25f, 0f)
        GlStateManager.rotate(-renderManager.playerViewX, 1f, 0f, 0f)
        GlStateManager.rotate(renderManager.playerViewY, 0f, 1f, 0f)
        GlStateManager.popMatrix()
        GlStateManager.disableLighting()


        if (seeThroughBlocks) {
            GlStateManager.enableDepth()
            GlStateManager.enableCull()
        }
    }

    /**
     * @author Mojang
     */
    private fun SkyHanniRenderWorldEvent.drawNametag(str: String, color: Color?) {
        val fontRenderer = Minecraft.getMinecraft().fontRendererObj
        val f1 = 0.02666667f
        GlStateManager.pushMatrix()
        GL11.glNormal3f(0f, 1f, 0f)
        GlStateManager.rotate(-Minecraft.getMinecraft().renderManager.playerViewY, 0f, 1f, 0f)
        GlStateManager.rotate(
            Minecraft.getMinecraft().renderManager.playerViewX,
            1f,
            0f,
            0f,
        )
        GlStateManager.scale(-f1, -f1, f1)
        GlStateManager.disableLighting()
        GlStateManager.depthMask(false)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        val i = 0
        val j = fontRenderer.getStringWidth(str) / 2
        GlStateManager.disableTexture2D()
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR)
        worldrenderer.pos((-j - 1).toDouble(), (-1 + i).toDouble(), 0.0).color(0f, 0f, 0f, 0.25f).endVertex()
        worldrenderer.pos((-j - 1).toDouble(), (8 + i).toDouble(), 0.0).color(0f, 0f, 0f, 0.25f).endVertex()
        worldrenderer.pos((j + 1).toDouble(), (8 + i).toDouble(), 0.0).color(0f, 0f, 0f, 0.25f).endVertex()
        worldrenderer.pos((j + 1).toDouble(), (-1 + i).toDouble(), 0.0).color(0f, 0f, 0f, 0.25f).endVertex()
        tessellator.draw()
        GlStateManager.enableTexture2D()
        val colorCode = color?.rgb ?: 553648127
        fontRenderer.drawString(str, -j, i, colorCode)
        GlStateManager.depthMask(true)
        fontRenderer.drawString(str, -j, i, -1)
        GlStateManager.enableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.popMatrix()
    }

    // modified from Autumn Client's TargetStrafe
    fun SkyHanniRenderWorldEvent.drawCircleWireframe(entity: Entity, rad: Double, color: Color) {
        GlStateManager.pushMatrix()
        GL11.glNormal3f(0f, 1f, 0f)

        GlStateManager.enableBlend()
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.enableAlpha()
        GlStateManager.disableTexture2D()
        GlStateManager.disableDepth()

        var il = 0.0
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        while (il < 0.05) {
            GL11.glLineWidth(2F)
            worldRenderer.begin(1, DefaultVertexFormats.POSITION)
            val renderManager = Minecraft.getMinecraft().renderManager
            val x: Double =
                entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.viewerPosX
            val y: Double =
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.viewerPosY
            val z: Double =
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.viewerPosZ
            val pix2 = Math.PI * 2.0
            for (i in 0..90) {
                color.bindColor()
                worldRenderer.pos(x + rad * cos(i * pix2 / 45.0), y + il, z + rad * sin(i * pix2 / 45.0)).endVertex()
            }
            tessellator.draw()
            il += 0.0006
        }

        GlStateManager.enableCull()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.popMatrix()
    }

    fun SkyHanniRenderWorldEvent.drawCircleFilled(
        entity: Entity,
        rad: Double,
        color: Color,
        depth: Boolean = true,
        segments: Int = 32,
    ) {
        val exactLocation = exactLocation(entity)
        drawCircleFilled(exactLocation.x, exactLocation.y, exactLocation.z, rad, color, depth, segments)
    }

    fun SkyHanniRenderWorldEvent.drawCircleFilled(
        locX: Double,
        locY: Double,
        locZ: Double,
        rad: Double,
        color: Color,
        depth: Boolean = true,
        segments: Int = 32,
    ) {
        GlStateManager.pushMatrix()
        GL11.glNormal3f(0f, 1f, 0f)

        GlStateManager.enableBlend()
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.enableAlpha()
        GlStateManager.disableTexture2D()
        if (!depth) GlStateManager.disableDepth()

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        val renderManager = Minecraft.getMinecraft().renderManager
        val x: Double = locX - renderManager.viewerPosX
        val y: Double = locY - renderManager.viewerPosY + 0.0020000000949949026
        val z: Double = locZ - renderManager.viewerPosZ

        worldRenderer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR)
        for (i in 0 until segments) {
            val angle1 = i * Math.PI * 2 / segments
            val angle2 = (i + 1) * Math.PI * 2 / segments

            worldRenderer.pos(x + rad * cos(angle1), y, z + rad * sin(angle1))
                .color(color.red, color.green, color.blue, color.alpha).endVertex()
            worldRenderer.pos(x + rad * cos(angle2), y, z + rad * sin(angle2))
                .color(color.red, color.green, color.blue, color.alpha).endVertex()
            worldRenderer.pos(x, y, z).color(color.red, color.green, color.blue, color.alpha).endVertex()
        }
        tessellator.draw()

        GlStateManager.enableCull()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.popMatrix()
    }

    fun SkyHanniRenderWorldEvent.drawCylinderInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
        height: Float,
    ) {
        drawCylinderInWorld(color, location.x, location.y, location.z, radius, height)
    }

    fun SkyHanniRenderWorldEvent.drawCylinderInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
        height: Float,
    ) {
        GlStateManager.pushMatrix()
        GL11.glNormal3f(0f, 1f, 0f)

        GlStateManager.enableDepth()
        GlStateManager.enableBlend()
        GlStateManager.depthFunc(GL11.GL_LEQUAL)
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.enableAlpha()
        GlStateManager.disableTexture2D()
        color.bindColor()
        bindCamera()

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION)
        var currentAngle = 0f
        val angleStep = 0.1f
        while (currentAngle < 2 * Math.PI) {
            val xOffset = radius * cos(currentAngle.toDouble()).toFloat()
            val zOffset = radius * sin(currentAngle.toDouble()).toFloat()
            worldRenderer.pos(x + xOffset, y + height, z + zOffset).endVertex()
            worldRenderer.pos(x + xOffset, y + 0, z + zOffset).endVertex()
            currentAngle += angleStep
        }
        worldRenderer.pos(x + radius, y + height, z).endVertex()
        worldRenderer.pos(x + radius, y + 0.0, z).endVertex()
        tessellator.draw()

        GlStateManager.enableCull()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.popMatrix()
    }

    fun SkyHanniRenderWorldEvent.drawPyramid(
        topPoint: LorenzVec,
        baseCenterPoint: LorenzVec,
        baseEdgePoint: LorenzVec,
        color: Color,
        depth: Boolean = true,
    ) {
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.disableTexture2D()
        GlStateManager.disableCull()
        GlStateManager.enableAlpha()
        if (!depth) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GlStateManager.depthMask(false)
        }
        GlStateManager.pushMatrix()

        color.bindColor()

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION)
        val inverseView = getViewerPos(partialTicks)
        translate(inverseView.negated())

        worldRenderer.pos(topPoint).endVertex()

        val corner1 = baseEdgePoint

        val cornerCenterVec = baseEdgePoint - baseCenterPoint

        val corner3 = baseCenterPoint - cornerCenterVec

        val baseTopVecNormalized = (topPoint - baseCenterPoint).normalize()

        val corner2 = baseTopVecNormalized.crossProduct(cornerCenterVec) + baseCenterPoint
        val corner4 = cornerCenterVec.crossProduct(baseTopVecNormalized) + baseCenterPoint

        worldRenderer.pos(corner1).endVertex()
        worldRenderer.pos(corner2).endVertex()

        worldRenderer.pos(corner2).endVertex()
        worldRenderer.pos(corner3).endVertex()

        worldRenderer.pos(corner3).endVertex()
        worldRenderer.pos(corner4).endVertex()

        worldRenderer.pos(corner4).endVertex()
        worldRenderer.pos(corner1).endVertex()

        tessellator.draw()

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)

        worldRenderer.pos(corner1).endVertex()
        worldRenderer.pos(corner4).endVertex()
        worldRenderer.pos(corner3).endVertex()
        worldRenderer.pos(corner2).endVertex()

        tessellator.draw()

        GlStateManager.popMatrix()
        GlStateManager.enableTexture2D()
        GlStateManager.enableCull()
        GlStateManager.disableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
        if (!depth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GlStateManager.depthMask(true)
        }
    }

    fun SkyHanniRenderWorldEvent.drawSphereInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
        segments: Int = 32,
    ) {
        drawSphereInWorld(color, location.x, location.y, location.z, radius, segments)
    }

    fun SkyHanniRenderWorldEvent.drawSphereInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
        segments: Int = 32,
    ) {
        GlStateManager.pushMatrix()
        GL11.glNormal3f(0f, 1f, 0f)

        GlStateManager.enableDepth()
        GlStateManager.enableBlend()
        GlStateManager.depthFunc(GL11.GL_LEQUAL)
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.enableAlpha()
        GlStateManager.disableTexture2D()
        color.bindColor()
        bindCamera()

        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)

        for (phi in 0 until segments) {
            for (theta in 0 until segments * 2) {
                val x1 = x + radius * sin(Math.PI * phi / segments) * cos(2.0 * Math.PI * theta / (segments * 2))
                val y1 = y + radius * cos(Math.PI * phi / segments)
                val z1 = z + radius * sin(Math.PI * phi / segments) * sin(2.0 * Math.PI * theta / (segments * 2))

                val x2 = x + radius * sin(Math.PI * (phi + 1) / segments) * cos(2.0 * Math.PI * theta / (segments * 2))
                val y2 = y + radius * cos(Math.PI * (phi + 1) / segments)
                val z2 = z + radius * sin(Math.PI * (phi + 1) / segments) * sin(2.0 * Math.PI * theta / (segments * 2))

                worldrenderer.pos(x1, y1, z1).endVertex()
                worldrenderer.pos(x2, y2, z2).endVertex()

                val x3 = x + radius * sin(Math.PI * (phi + 1) / segments) * cos(2.0 * Math.PI * (theta + 1) / (segments * 2))
                val y3 = y + radius * cos(Math.PI * (phi + 1) / segments)
                val z3 = z + radius * sin(Math.PI * (phi + 1) / segments) * sin(2.0 * Math.PI * (theta + 1) / (segments * 2))

                val x4 = x + radius * sin(Math.PI * phi / segments) * cos(2.0 * Math.PI * (theta + 1) / (segments * 2))
                val y4 = y + radius * cos(Math.PI * phi / segments)
                val z4 = z + radius * sin(Math.PI * phi / segments) * sin(2.0 * Math.PI * (theta + 1) / (segments * 2))

                worldrenderer.pos(x3, y3, z3).endVertex()
                worldrenderer.pos(x4, y4, z4).endVertex()
            }
        }

        tessellator.draw()

        GlStateManager.enableCull()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.popMatrix()
    }

    fun SkyHanniRenderWorldEvent.drawSphereWireframeInWorld(
        color: Color,
        location: LorenzVec,
        radius: Float,
        segments: Int = 32,
    ) {
        drawSphereWireframeInWorld(color, location.x, location.y, location.z, radius, segments)
    }

    fun SkyHanniRenderWorldEvent.drawSphereWireframeInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
        segments: Int = 32,
    ) {
        GlStateManager.pushMatrix()
        GL11.glNormal3f(0f, 1f, 0f)

        GlStateManager.disableTexture2D()
        color.bindColor()
        bindCamera()

        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION)

        for (phi in 0 until segments) {
            for (theta in 0 until segments * 2) {
                val x1 = x + radius * sin(Math.PI * phi / segments) * cos(2.0 * Math.PI * theta / (segments * 2))
                val y1 = y + radius * cos(Math.PI * phi / segments)
                val z1 = z + radius * sin(Math.PI * phi / segments) * sin(2.0 * Math.PI * theta / (segments * 2))

                val x2 = x + radius * sin(Math.PI * (phi + 1) / segments) * cos(2.0 * Math.PI * theta / (segments * 2))
                val y2 = y + radius * cos(Math.PI * (phi + 1) / segments)
                val z2 = z + radius * sin(Math.PI * (phi + 1) / segments) * sin(2.0 * Math.PI * theta / (segments * 2))

                val x3 = x + radius * sin(Math.PI * (phi + 1) / segments) * cos(2.0 * Math.PI * (theta + 1) / (segments * 2))
                val y3 = y + radius * cos(Math.PI * (phi + 1) / segments)
                val z3 = z + radius * sin(Math.PI * (phi + 1) / segments) * sin(2.0 * Math.PI * (theta + 1) / (segments * 2))

                val x4 = x + radius * sin(Math.PI * phi / segments) * cos(2.0 * Math.PI * (theta + 1) / (segments * 2))
                val y4 = y + radius * cos(Math.PI * phi / segments)
                val z4 = z + radius * sin(Math.PI * phi / segments) * sin(2.0 * Math.PI * (theta + 1) / (segments * 2))

                worldrenderer.pos(x1, y1, z1).endVertex()
                worldrenderer.pos(x2, y2, z2).endVertex()

                worldrenderer.pos(x2, y2, z2).endVertex()
                worldrenderer.pos(x3, y3, z3).endVertex()

                worldrenderer.pos(x3, y3, z3).endVertex()
                worldrenderer.pos(x4, y4, z4).endVertex()

                worldrenderer.pos(x4, y4, z4).endVertex()
                worldrenderer.pos(x1, y1, z1).endVertex()
            }
        }

        tessellator.draw()

        GlStateManager.enableTexture2D()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.popMatrix()
    }

    fun SkyHanniRenderWorldEvent.drawDynamicText(
        location: LorenzVec,
        text: String,
        scaleMultiplier: Double,
        yOff: Float = 0f,
        hideTooCloseAt: Double = 4.5,
        smallestDistanceVew: Double = 5.0,
        seeThroughBlocks: Boolean = true,
        ignoreY: Boolean = false,
        maxDistance: Int? = null,
    ) {
        val viewer = Minecraft.getMinecraft().renderViewEntity ?: return
        val player = MinecraftCompat.localPlayerOrNull ?: return

        val x = location.x
        val y = location.y
        val z = location.z

        val renderOffsetX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks
        val renderOffsetY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks
        val renderOffsetZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks
        val eyeHeight = player.getEyeHeight()

        val dX = (x - renderOffsetX) * (x - renderOffsetX)
        val dY = (y - (renderOffsetY + eyeHeight)) * (y - (renderOffsetY + eyeHeight))
        val dZ = (z - renderOffsetZ) * (z - renderOffsetZ)
        val distToPlayerSq = dX + dY + dZ
        var distToPlayer = sqrt(distToPlayerSq)
        // TODO this is optional maybe?
        distToPlayer = distToPlayer.coerceAtLeast(smallestDistanceVew)

        if (distToPlayer < hideTooCloseAt) return
        maxDistance?.let {
            if (seeThroughBlocks && distToPlayer > it) return
        }

        val distRender = distToPlayer.coerceAtMost(50.0)

        var scale = distRender / 12
        scale *= scaleMultiplier

        val resultX = renderOffsetX + (x + 0.5 - renderOffsetX) / (distToPlayer / distRender)
        val resultY = if (ignoreY) y * distToPlayer / distRender else renderOffsetY + eyeHeight +
            (y + 20 * distToPlayer / 300 - (renderOffsetY + eyeHeight)) / (distToPlayer / distRender)
        val resultZ = renderOffsetZ + (z + 0.5 - renderOffsetZ) / (distToPlayer / distRender)

        val renderLocation = LorenzVec(resultX, resultY, resultZ)

        renderText(renderLocation, "§f$text", scale, !seeThroughBlocks, true, yOff)
    }

    private fun SkyHanniRenderWorldEvent.renderText(
        location: LorenzVec,
        text: String,
        scale: Double,
        seeThroughBlocks: Boolean,
        shadow: Boolean,
        yOff: Float,
    ) {
        if (!seeThroughBlocks) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDepthMask(false)
        }
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)

        val minecraft = Minecraft.getMinecraft()
        val fontRenderer = minecraft.fontRendererObj
        val renderManager = minecraft.renderManager

        GlStateManager.translate(
            location.x - renderManager.viewerPosX,
            location.y - renderManager.viewerPosY,
            location.z - renderManager.viewerPosZ,
        )
        GlStateManager.color(1f, 1f, 1f, 0.5f)
        GlStateManager.rotate(-renderManager.playerViewY, 0f, 1f, 0f)
        GlStateManager.rotate(renderManager.playerViewX, 1f, 0f, 0f)
        GlStateManager.scale(-scale / 25, -scale / 25, scale / 25)
        val stringWidth = fontRenderer.getStringWidth(text)
        fontRenderer.drawString(
            text,
            (-stringWidth / 2).toFloat(),
            yOff,
            0,
            shadow,
        )
        GlStateManager.color(1f, 1f, 1f)
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
        if (!seeThroughBlocks) {
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glDepthMask(true)
        }
    }

    // TODO add chroma color support
    fun SkyHanniRenderWorldEvent.drawEdges(location: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        LineDrawer.draw3D(this, lineWidth, depth) {
            drawEdges(location, color)
        }
    }

    // TODO add chroma color support
    fun SkyHanniRenderWorldEvent.drawEdges(axisAlignedBB: AxisAlignedBB, color: Color, lineWidth: Int, depth: Boolean) {
        LineDrawer.draw3D(this, lineWidth, depth) {
            drawEdges(axisAlignedBB, color)
        }
    }

    fun SkyHanniRenderWorldEvent.draw3DLine(
        p1: LorenzVec,
        p2: LorenzVec,
        color: ChromaColour,
        lineWidth: Int,
        depth: Boolean,
    ) {
        draw3DLine(p1, p2, color.toColor(), lineWidth, depth)
    }

    fun SkyHanniRenderWorldEvent.draw3DLine(
        p1: LorenzVec,
        p2: LorenzVec,
        color: Color,
        lineWidth: Int,
        depth: Boolean,
    ) = LineDrawer.draw3D(this, lineWidth, depth) {
        draw3DLine(p1, p2, color)
    }

    fun SkyHanniRenderWorldEvent.outlineTopFace(
        boundingBox: AxisAlignedBB,
        lineWidth: Int,
        color: Color,
        depth: Boolean,
    ) {
        val (cornerOne, cornerTwo, cornerThree, cornerFour) = boundingBox.getCornersAtHeight(boundingBox.maxY)
        draw3DLine(cornerOne, cornerTwo, color, lineWidth, depth)
        draw3DLine(cornerTwo, cornerThree, color, lineWidth, depth)
        draw3DLine(cornerThree, cornerFour, color, lineWidth, depth)
        draw3DLine(cornerFour, cornerOne, color, lineWidth, depth)
    }

    // TODO add chroma color support
    fun SkyHanniRenderWorldEvent.drawHitbox(
        boundingBox: AxisAlignedBB,
        color: Color,
        lineWidth: Int = 3,
        depth: Boolean = true,
    ) {
        val cornersTop = boundingBox.getCornersAtHeight(boundingBox.maxY)
        val cornersBottom = boundingBox.getCornersAtHeight(boundingBox.minY)

        // Draw lines for the top and bottom faces
        for (i in 0..3) {
            this.draw3DLine(cornersTop[i], cornersTop[(i + 1) % 4], color, lineWidth, depth)
            this.draw3DLine(cornersBottom[i], cornersBottom[(i + 1) % 4], color, lineWidth, depth)
        }

        // Draw lines connecting the top and bottom faces
        for (i in 0..3) {
            this.draw3DLine(cornersBottom[i], cornersTop[i], color, lineWidth, depth)
        }
    }

    fun SkyHanniRenderWorldEvent.drawLineToEye(location: LorenzVec, color: ChromaColour, lineWidth: Int, depth: Boolean) {
        drawLineToEye(location, color.toColor(), lineWidth, depth)
    }

    fun SkyHanniRenderWorldEvent.drawLineToEye(location: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        draw3DLine(exactPlayerEyeLocation(), location, color, lineWidth, depth)
    }

    fun SkyHanniRenderWorldEvent.draw3DPathWithWaypoint(
        path: Graph,
        colorLine: Color,
        lineWidth: Int,
        depth: Boolean,
        startAtEye: Boolean = true,
        textSize: Double = 1.0,
        waypointColor: Color =
            (path.lastOrNull()?.name?.getFirstColorCode()?.toLorenzColor() ?: LorenzColor.WHITE).toColor(),
        bezierPoint: Double = 1.0,
        showNodeNames: Boolean = false,
        markLastBlock: Boolean = true,
    ) {
        if (path.isEmpty()) return
        val points = if (startAtEye) {
            listOf(
                this.exactPlayerEyeLocation() + MinecraftCompat.localPlayer.getLook(this.partialTicks)
                    .toLorenzVec()
                    /* .rotateXZ(-Math.PI / 72.0) */
                    .times(2),
            )
        } else {
            emptyList()
        } + path.toPositionsList().map { it.add(0.5, 0.5, 0.5) }
        LineDrawer.draw3D(this, lineWidth, depth) {
            drawPath(
                points,
                colorLine,
                bezierPoint,
            )
        }
        if (showNodeNames) {
            path.filter { it.name?.isNotEmpty() == true }.forEach {
                this.drawDynamicText(it.position, it.name!!, textSize)
            }
        }
        if (markLastBlock) {
            val last = path.last()
            drawWaypointFilled(last.position, waypointColor, seeThroughBlocks = true)
        }
    }

    fun getViewerPos(partialTicks: Float) =
        Minecraft.getMinecraft().renderViewEntity?.let { exactLocation(it, partialTicks) } ?: LorenzVec()

    fun AxisAlignedBB.expandBlock(n: Int = 1) = expand(LorenzVec.expandVector * n)
    fun AxisAlignedBB.inflateBlock(n: Int = 1) = expand(LorenzVec.expandVector * -n)

    fun exactLocation(entity: Entity, partialTicks: Float): LorenzVec {
        if (entity.isDead) return entity.getLorenzVec()
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks
        return LorenzVec(x, y, z)
    }

    fun SkyHanniRenderWorldEvent.exactLocation(entity: Entity) = exactLocation(entity, partialTicks)

    fun SkyHanniRenderWorldEvent.exactPlayerEyeLocation(): LorenzVec {
        val player = MinecraftCompat.localPlayer
        val eyeHeight = player.getEyeHeight().toDouble()
        return exactLocation(player).add(y = eyeHeight)
    }

    fun SkyHanniRenderWorldEvent.exactBoundingBox(entity: Entity): AxisAlignedBB {
        if (entity.isDead) return entity.entityBoundingBox
        val offset = exactLocation(entity) - entity.getLorenzVec()
        return entity.entityBoundingBox.offset(offset.x, offset.y, offset.z)
    }

    fun SkyHanniRenderWorldEvent.exactPlayerEyeLocation(player: Entity): LorenzVec {
        val add = if (player.isSneaking) LorenzVec(0.0, 1.54, 0.0) else LorenzVec(0.0, 1.62, 0.0)
        return exactLocation(player) + add
    }

    private fun Color.bindColor() =
        GlStateManager.color(this.red / 255f, this.green / 255f, this.blue / 255f, this.alpha / 255f)

    private fun bindCamera() {
        val renderManager = Minecraft.getMinecraft().renderManager
        val viewer = renderManager.viewerPosX
        val viewY = renderManager.viewerPosY
        val viewZ = renderManager.viewerPosZ
        GlStateManager.translate(-viewer, -viewY, -viewZ)
    }

    fun WorldRenderer.pos(vec: LorenzVec): WorldRenderer = this.pos(vec.x, vec.y, vec.z)

    fun translate(vec: LorenzVec) = GlStateManager.translate(vec.x, vec.y, vec.z)
}
