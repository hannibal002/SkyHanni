package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsY
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.moulberry.moulconfig.internal.TextRenderUtils
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.inventory.Slot
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.DurationUnit

object RenderUtils {

    private val beaconBeam = ResourceLocation("textures/entity/beacon_beam.png")

    infix fun Slot.highlight(color: LorenzColor) {
        highlight(color.toColor())
    }

    infix fun Slot.highlight(color: Color) {
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.pushAttrib()
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GlStateManager.pushMatrix()
        // TODO don't use z
        GlStateManager.translate(0f, 0f, 110 + Minecraft.getMinecraft().renderItem.zLevel)
        Gui.drawRect(
            this.xDisplayPosition,
            this.yDisplayPosition,
            this.xDisplayPosition + 16,
            this.yDisplayPosition + 16,
            color.rgb
        )
        GlStateManager.popMatrix()
        GlStateManager.popAttrib()
    }

    fun LorenzRenderWorldEvent.drawColor(
        location: LorenzVec,
        color: LorenzColor,
        beacon: Boolean = false,
        alpha: Float = -1f,
    ) {
        drawColor(location, color.toColor(), beacon, alpha)
    }

    fun LorenzRenderWorldEvent.drawColor(
        location: LorenzVec,
        color: Color,
        beacon: Boolean = false,
        alpha: Float = -1f,
    ) {
        val (viewerX, viewerY, viewerZ) = getViewerPos(partialTicks)
        val x = location.x - viewerX
        val y = location.y - viewerY
        val z = location.z - viewerZ
        val distSq = x * x + y * y + z * z
        val realAlpha = if (alpha == -1f) {
            (0.1f + 0.005f * distSq.toFloat()).coerceAtLeast(0.2f)
        } else {
            alpha
        }
        GlStateManager.disableDepth()
        GlStateManager.disableCull()
        drawFilledBoundingBox(
            AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1).expandBlock(),
            color,
            realAlpha
        )
        GlStateManager.disableTexture2D()
        if (distSq > 5 * 5 && beacon) renderBeaconBeam(x, y + 1, z, color.rgb, 1.0f, partialTicks)
        GlStateManager.disableLighting()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.enableCull()
    }

    fun getViewerPos(partialTicks: Float) = exactLocation(Minecraft.getMinecraft().renderViewEntity, partialTicks)

    fun AxisAlignedBB.expandBlock() = expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)

    /**
     * Taken from NotEnoughUpdates under Creative Commons Attribution-NonCommercial 3.0
     * https://github.com/Moulberry/NotEnoughUpdates/blob/master/LICENSE
     * @author Moulberry
     * @author Mojang
     */
    fun renderBeaconBeam(x: Double, y: Double, z: Double, rgb: Int, alphaMultiplier: Float, partialTicks: Float) {
        val height = 300
        val bottomOffset = 0
        val topOffset = bottomOffset + height
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        Minecraft.getMinecraft().textureManager.bindTexture(beaconBeam)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10497.0f)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10497.0f)
        GlStateManager.disableLighting()
        GlStateManager.enableCull()
        GlStateManager.enableTexture2D()
        GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        val time = Minecraft.getMinecraft().theWorld.totalWorldTime + partialTicks.toDouble()
        val d1 = MathHelper.func_181162_h(
            -time * 0.2 - MathHelper.floor_double(-time * 0.1)
                .toDouble()
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
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
        worldrenderer.pos(x + d4, y + topOffset, z + d5).tex(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + d4, y + bottomOffset, z + d5).tex(1.0, d14).color(r, g, b, 1.0f).endVertex()
        worldrenderer.pos(x + d6, y + bottomOffset, z + d7).tex(0.0, d14).color(r, g, b, 1.0f).endVertex()
        worldrenderer.pos(x + d6, y + topOffset, z + d7).tex(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + d10, y + topOffset, z + d11).tex(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + d10, y + bottomOffset, z + d11).tex(1.0, d14).color(r, g, b, 1.0f).endVertex()
        worldrenderer.pos(x + d8, y + bottomOffset, z + d9).tex(0.0, d14).color(r, g, b, 1.0f).endVertex()
        worldrenderer.pos(x + d8, y + topOffset, z + d9).tex(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + d6, y + topOffset, z + d7).tex(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + d6, y + bottomOffset, z + d7).tex(1.0, d14).color(r, g, b, 1.0f).endVertex()
        worldrenderer.pos(x + d10, y + bottomOffset, z + d11).tex(0.0, d14).color(r, g, b, 1.0f).endVertex()
        worldrenderer.pos(x + d10, y + topOffset, z + d11).tex(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + d8, y + topOffset, z + d9).tex(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + d8, y + bottomOffset, z + d9).tex(1.0, d14).color(r, g, b, 1.0f).endVertex()
        worldrenderer.pos(x + d4, y + bottomOffset, z + d5).tex(0.0, d14).color(r, g, b, 1.0f).endVertex()
        worldrenderer.pos(x + d4, y + topOffset, z + d5).tex(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .endVertex()
        tessellator.draw()
        GlStateManager.disableCull()
        val d12 = -1.0 + d1
        val d13 = height + d12
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR)
        worldrenderer.pos(x + 0.2, y + topOffset, z + 0.2).tex(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + 0.2, y + bottomOffset, z + 0.2).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
        worldrenderer.pos(x + 0.8, y + bottomOffset, z + 0.2).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
        worldrenderer.pos(x + 0.8, y + topOffset, z + 0.2).tex(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + 0.8, y + topOffset, z + 0.8).tex(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + 0.8, y + bottomOffset, z + 0.8).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
        worldrenderer.pos(x + 0.2, y + bottomOffset, z + 0.8).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
        worldrenderer.pos(x + 0.2, y + topOffset, z + 0.8).tex(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + 0.8, y + topOffset, z + 0.2).tex(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + 0.8, y + bottomOffset, z + 0.2).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
        worldrenderer.pos(x + 0.8, y + bottomOffset, z + 0.8).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
        worldrenderer.pos(x + 0.8, y + topOffset, z + 0.8).tex(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + 0.2, y + topOffset, z + 0.8).tex(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .endVertex()
        worldrenderer.pos(x + 0.2, y + bottomOffset, z + 0.8).tex(1.0, d12).color(r, g, b, 0.25f).endVertex()
        worldrenderer.pos(x + 0.2, y + bottomOffset, z + 0.2).tex(0.0, d12).color(r, g, b, 0.25f).endVertex()
        worldrenderer.pos(x + 0.2, y + topOffset, z + 0.2).tex(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .endVertex()
        tessellator.draw()
    }

    fun LorenzRenderWorldEvent.drawString(
        location: LorenzVec,
        text: String,
        seeThroughBlocks: Boolean = false,
        color: Color? = null,
    ) {
        GlStateManager.alphaFunc(516, 0.1f)
        GlStateManager.pushMatrix()
        val viewer = Minecraft.getMinecraft().renderViewEntity
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
        GlStateManager.rotate(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(renderManager.playerViewX, 1.0f, 0.0f, 0.0f)
        GlStateManager.translate(0f, -0.25f, 0f)
        GlStateManager.rotate(-renderManager.playerViewX, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
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
    fun drawNametag(str: String, color: Color?) {
        val fontRenderer = Minecraft.getMinecraft().fontRendererObj
        val f1 = 0.02666667f
        GlStateManager.pushMatrix()
        GL11.glNormal3f(0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(-Minecraft.getMinecraft().renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(
            Minecraft.getMinecraft().renderManager.playerViewX,
            1.0f,
            0.0f,
            0.0f
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
        worldrenderer.pos((-j - 1).toDouble(), (-1 + i).toDouble(), 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        worldrenderer.pos((-j - 1).toDouble(), (8 + i).toDouble(), 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        worldrenderer.pos((j + 1).toDouble(), (8 + i).toDouble(), 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        worldrenderer.pos((j + 1).toDouble(), (-1 + i).toDouble(), 0.0).color(0.0f, 0.0f, 0.0f, 0.25f).endVertex()
        tessellator.draw()
        GlStateManager.enableTexture2D()
        val colorCode = color?.rgb ?: 553648127
        fontRenderer.drawString(str, -j, i, colorCode)
        GlStateManager.depthMask(true)
        fontRenderer.drawString(str, -j, i, -1)
        GlStateManager.enableBlend()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.popMatrix()
    }

    /**
     * @author Mojang
     */
    fun drawLabel(
        pos: LorenzVec,
        text: String,
        partialTicks: Float,
        shadow: Boolean = false,
        scale: Float = 1f,
        yOff: Float = 0f,
        debug: Boolean = false,
    ) {
        val minecraft = Minecraft.getMinecraft()
        val player = minecraft.thePlayer
        val x =
            pos.x - player.lastTickPosX + (pos.x - player.posX - (pos.x - player.lastTickPosX)) * partialTicks
        val y =
            pos.y - player.lastTickPosY + (pos.y - player.posY - (pos.y - player.lastTickPosY)) * partialTicks
        val z =
            pos.z - player.lastTickPosZ + (pos.z - player.posZ - (pos.z - player.lastTickPosZ)) * partialTicks

        //7 – 25

        val translate = LorenzVec(x, y, z)
        val length = translate.length().toFloat()

        var finalText = text
        var factor = 1f
        var finalScale = scale
        if (debug) {
            finalText = "$text ${length.toInt()}"
            if (length < 8) {
                factor = 8 / length
            }
            if (length > 15) {
                factor = 15 / length
            }
            finalScale = scale * sqrt(factor.toDouble()).toFloat()
        }

        val f1 = 0.0266666688

        val width = minecraft.fontRendererObj.getStringWidth(finalText) / 2
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, z)
        GL11.glNormal3f(0f, 1f, 0f)
        val renderManager = minecraft.renderManager
        GlStateManager.rotate(-renderManager.playerViewY, 0f, 1f, 0f)
        GlStateManager.rotate(renderManager.playerViewX, 1f, 0f, 0f)
        GlStateManager.scale(-f1, -f1, -f1)
        GlStateManager.scale(finalScale, finalScale, finalScale)
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.enableTexture2D()
        minecraft.fontRendererObj.drawString(
            finalText,
            (-width).toFloat(),
            yOff,
            LorenzColor.WHITE.toColor().rgb,
            shadow
        )
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    fun interpolate(currentValue: Double, lastValue: Double, multiplier: Float): Double {
        return lastValue + (currentValue - lastValue) * multiplier
    }

    fun Position.transform(): Pair<Int, Int> {
        GlStateManager.translate(getAbsX().toFloat(), getAbsY().toFloat(), 0F)
        GlStateManager.scale(effectiveScale, effectiveScale, 1F)
        val x = ((Utils.getMouseX() - getAbsX()) / effectiveScale).toInt()
        val y = ((Utils.getMouseY() - getAbsY()) / effectiveScale).toInt()
        return x to y
    }

    fun Position.renderString(string: String?, offsetX: Int = 0, offsetY: Int = 0, posLabel: String) {
        if (string == null) return
        if (string == "") return
        val x = renderString0(string, offsetX, offsetY, isCenter)
        GuiEditManager.add(this, posLabel, x, 10)
    }

    private fun Position.renderString0(string: String?, offsetX: Int = 0, offsetY: Int = 0, centered: Boolean): Int {
        val display = "§f$string"
        GlStateManager.pushMatrix()
        transform()
        val minecraft = Minecraft.getMinecraft()
        val renderer = minecraft.renderManager.fontRenderer

        GlStateManager.translate(offsetX + 1.0, offsetY + 1.0, 0.0)

        if (centered) {
            val strLen: Int = renderer.getStringWidth(string)
            val x2 = offsetX - strLen / 2f
            GL11.glTranslatef(x2, 0f, 0f)
            renderer.drawStringWithShadow(display, 0f, 0f, 0)
            GL11.glTranslatef(-x2, 0f, 0f)
        } else {
            renderer.drawStringWithShadow(display, 0f, 0f, 0)
        }


        GlStateManager.popMatrix()

        return renderer.getStringWidth(display)
    }

    fun Position.renderStrings(list: List<String>, extraSpace: Int = 0, posLabel: String) {
        if (list.isEmpty()) return

        var offsetY = 0
        var longestX = 0
        for (s in list) {
            val x = renderString0(s, offsetY = offsetY, centered = false)
            if (x > longestX) {
                longestX = x
            }
            offsetY += 10 + extraSpace
        }
        GuiEditManager.add(this, posLabel, longestX, offsetY)
    }

    fun Position.renderRenderables(
        renderables: List<Renderable>,
        extraSpace: Int = 0,
        itemScale: Double = 1.0,
        posLabel: String,
    ) {
        if (renderables.isEmpty()) return
        val list = mutableListOf<List<Any>>()
        for (line in renderables) {
            list.addAsSingletonList(line)
        }
        renderStringsAndItems(list, extraSpace, itemScale, posLabel)
    }

    /**
     * Accepts a list of lines to print.
     * Each line is a list of things to print. Can print String or ItemStack objects.
     */
    fun Position.renderStringsAndItems(
        list: List<List<Any?>>,
        extraSpace: Int = 0,
        itemScale: Double = 1.0,
        posLabel: String,
    ) {
        if (list.isEmpty()) return

        var offsetY = 0
        var longestX = 0
        try {
            for (line in list) {
                val x = renderLine(line, offsetY, itemScale)
                if (x > longestX) {
                    longestX = x
                }
                offsetY += 10 + extraSpace + 2
            }
        } catch (e: NullPointerException) {
            println(" ")
            for (innerList in list) {
                println("new inner list:")
                for (any in innerList) {
                    println("any: '$any'")
                }
            }
            e.printStackTrace()
            LorenzUtils.debug("NPE in renderStringsAndItems!")
        }
        GuiEditManager.add(this, posLabel, longestX, offsetY)
    }

    /**
     * Accepts a single line to print.
     * This  line is a list of things to print. Can print String or ItemStack objects.
     */
    fun Position.renderSingleLineWithItems(list: List<Any?>, itemScale: Double = 1.0, posLabel: String) {
        if (list.isEmpty()) return
        val longestX = renderLine(list, 0, itemScale)
        GuiEditManager.add(this, posLabel, longestX, 10)
    }

    private fun Position.renderLine(line: List<Any?>, offsetY: Int, itemScale: Double = 1.0): Int {
        GlStateManager.pushMatrix()
        val (x, y) = transform()
        GlStateManager.translate(0f, offsetY.toFloat(), 0F)
        var offsetX = 0
        Renderable.withMousePosition(x, y) {
            for (any in line) {
                val renderable = Renderable.fromAny(any, itemScale = itemScale)
                    ?: throw RuntimeException("Unknown render object: $any")
                renderable.render(offsetX, offsetY)
                offsetX += renderable.width
                GlStateManager.translate(renderable.width.toFloat(), 0F, 0F)
            }
        }
        GlStateManager.popMatrix()
        return offsetX
    }

    // totally not modified Autumn Client's TargetStrafe
    fun drawCircle(entity: Entity, partialTicks: Float, rad: Double, color: Color) {
        GlStateManager.pushMatrix()
        GL11.glNormal3f(0.0f, 1.0f, 0.0f)

        GlStateManager.enableDepth()
        GlStateManager.enableBlend()
        GlStateManager.depthFunc(GL11.GL_LEQUAL)
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.enableAlpha()
        GlStateManager.disableTexture2D()

        GlStateManager.disableDepth()

        var il = 0.0
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        while (il < 0.05) {
            GlStateManager.pushMatrix()
            GlStateManager.disableTexture2D()
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
            GlStateManager.enableTexture2D()
            GlStateManager.popMatrix()
            il += 0.0006
        }

        GlStateManager.enableDepth()

        GlStateManager.enableCull()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.popMatrix()
    }

    fun drawCylinderInWorld(
        color: Color,
        x: Double,
        y: Double,
        z: Double,
        radius: Float,
        height: Float,
        partialTicks: Float,
    ) {
        GlStateManager.pushMatrix()
        GL11.glNormal3f(0.0f, 1.0f, 0.0f)

        GlStateManager.enableDepth()
        GlStateManager.enableBlend()
        GlStateManager.depthFunc(GL11.GL_LEQUAL)
        GlStateManager.disableCull()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.enableAlpha()
        GlStateManager.disableTexture2D()
        color.bindColor()

        var x1 = x
        var y1 = y
        var z1 = z
        val renderViewEntity = Minecraft.getMinecraft().renderViewEntity
        val viewX =
            renderViewEntity.prevPosX + (renderViewEntity.posX - renderViewEntity.prevPosX) * partialTicks.toDouble()
        val viewY =
            renderViewEntity.prevPosY + (renderViewEntity.posY - renderViewEntity.prevPosY) * partialTicks.toDouble()
        val viewZ =
            renderViewEntity.prevPosZ + (renderViewEntity.posZ - renderViewEntity.prevPosZ) * partialTicks.toDouble()
        x1 -= viewX
        y1 -= viewY
        z1 -= viewZ
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION)
        var currentAngle = 0f
        val angleStep = 0.1f
        while (currentAngle < 2 * Math.PI) {
            val xOffset = radius * cos(currentAngle.toDouble()).toFloat()
            val zOffset = radius * sin(currentAngle.toDouble()).toFloat()
            worldrenderer.pos(x1 + xOffset, y1 + height, z1 + zOffset).endVertex()
            worldrenderer.pos(x1 + xOffset, y1 + 0, z1 + zOffset).endVertex()
            currentAngle += angleStep
        }
        worldrenderer.pos(x1 + radius, y1 + height, z1).endVertex()
        worldrenderer.pos(x1 + radius, y1 + 0.0, z1).endVertex()
        tessellator.draw()

        GlStateManager.enableCull()
        GlStateManager.enableTexture2D()
        GlStateManager.enableDepth()
        GlStateManager.disableBlend()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.popMatrix()
    }

    private fun Color.bindColor() =
        GlStateManager.color(this.red / 255f, this.green / 255f, this.blue / 255f, this.alpha / 255f)

    fun drawStringScaledMaxWidth(
        str: String?,
        fr: FontRenderer,
        x: Float,
        y: Float,
        shadow: Boolean,
        len: Int,
        colour: Int,
    ) {
        val strLen = fr.getStringWidth(str)
        var factor = len / strLen.toFloat()
        factor = 1f.coerceAtMost(factor)
        TextRenderUtils.drawStringScaled(str, fr, x, y, shadow, colour, factor)
    }

    fun LorenzRenderWorldEvent.drawDynamicText(
        location: LorenzVec,
        text: String,
        scaleMultiplier: Double,
        yOff: Float = 0f,
        hideTooCloseAt: Double = 4.5,
        smallestDistanceVew: Double = 5.0,
        ignoreBlocks: Boolean = true,
    ) {
        val thePlayer = Minecraft.getMinecraft().thePlayer
        val x = location.x
        val y = location.y
        val z = location.z

        val render = Minecraft.getMinecraft().renderViewEntity
        val renderOffsetX = render.lastTickPosX + (render.posX - render.lastTickPosX) * partialTicks
        val renderOffsetY = render.lastTickPosY + (render.posY - render.lastTickPosY) * partialTicks
        val renderOffsetZ = render.lastTickPosZ + (render.posZ - render.lastTickPosZ) * partialTicks
        val eyeHeight = thePlayer.eyeHeight

        val distToPlayerSq =
            (x - renderOffsetX) * (x - renderOffsetX) + (y - (renderOffsetY + eyeHeight)) * (y - (renderOffsetY + eyeHeight)) + (z - renderOffsetZ) * (z - renderOffsetZ)
        var distToPlayer = sqrt(distToPlayerSq)
        //TODO this is optional maybe?
        distToPlayer = distToPlayer.coerceAtLeast(smallestDistanceVew)

        if (distToPlayer < hideTooCloseAt) return

        val distRender = distToPlayer.coerceAtMost(50.0)

        val resultX = renderOffsetX + (x + 0.5 - renderOffsetX) / (distToPlayer / distRender)
        val resultY =
            (renderOffsetY + eyeHeight) + (y + 20 * distToPlayer / 300 - (renderOffsetY + eyeHeight)) / (distToPlayer / distRender)
        val resultZ = renderOffsetZ + (z + 0.5 - renderOffsetZ) / (distToPlayer / distRender)

        val renderLocation = LorenzVec(resultX, resultY, resultZ)
        var scale = distRender / 12
        scale *= scaleMultiplier
        render(renderLocation, "§f$text", scale, !ignoreBlocks, true, yOff)
    }

    private fun render(
        location: LorenzVec,
        text: String,
        scale: Double,
        depthTest: Boolean,
        shadow: Boolean,
        yOff: Float,
    ) {
        if (!depthTest) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDepthMask(false)
        }
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

        val minecraft = Minecraft.getMinecraft()
        val fontRenderer = minecraft.fontRendererObj
        val renderManager = minecraft.renderManager

        GlStateManager.translate(
            location.x - renderManager.viewerPosX,
            location.y - renderManager.viewerPosY,
            location.z - renderManager.viewerPosZ
        )
        GlStateManager.color(1f, 1f, 1f, 0.5f)
        GlStateManager.rotate(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(renderManager.playerViewX, 1.0f, 0.0f, 0.0f)
        GlStateManager.scale(-scale / 25, -scale / 25, scale / 25)
        val stringWidth = fontRenderer.getStringWidth(text)
        if (shadow) {
            fontRenderer.drawStringWithShadow(
                text,
                (-stringWidth / 2).toFloat(),
                yOff,
                0
            )
        } else {
            fontRenderer.drawString(
                text,
                -stringWidth / 2,
                0,
                0
            )
        }
        GlStateManager.color(1f, 1f, 1f)
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
        if (!depthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glDepthMask(true)
        }
    }

    fun LorenzRenderWorldEvent.draw3DLine(p1: LorenzVec, p2: LorenzVec, color: Color, lineWidth: Int, depth: Boolean) {
        GlStateManager.disableCull()

        val render = Minecraft.getMinecraft().renderViewEntity
        val worldRenderer = Tessellator.getInstance().worldRenderer
        val realX = render.lastTickPosX + (render.posX - render.lastTickPosX) * partialTicks
        val realY = render.lastTickPosY + (render.posY - render.lastTickPosY) * partialTicks
        val realZ = render.lastTickPosZ + (render.posZ - render.lastTickPosZ) * partialTicks
        GlStateManager.pushMatrix()
        GlStateManager.translate(-realX, -realY, -realZ)
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GL11.glLineWidth(lineWidth.toFloat())
        if (!depth) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GlStateManager.depthMask(false)
        }
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        worldRenderer.pos(p1.x, p1.y, p1.z).endVertex()
        worldRenderer.pos(p2.x, p2.y, p2.z).endVertex()
        Tessellator.getInstance().draw()
        GlStateManager.translate(realX, realY, realZ)
        if (!depth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GlStateManager.depthMask(true)
        }
        GlStateManager.disableBlend()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.popMatrix()
        GlStateManager.disableLighting()
        GlStateManager.enableDepth()
    }

    fun LorenzRenderWorldEvent.exactLocation(entity: Entity) = exactLocation(entity, partialTicks)

    fun LorenzRenderWorldEvent.exactPlayerEyeLocation(): LorenzVec {
        val player = Minecraft.getMinecraft().thePlayer
        val add = if (player.isSneaking) LorenzVec(0.0, 1.54, 0.0) else LorenzVec(0.0, 1.62, 0.0)
        return exactLocation(player).add(add)
    }

    fun exactLocation(entity: Entity, partialTicks: Float): LorenzVec {
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks
        return LorenzVec(x, y, z)
    }

    fun drawFilledBoundingBox(aabb: AxisAlignedBB, c: Color, alphaMultiplier: Float = 1f) {
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.disableTexture2D()
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        GlStateManager.color(c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f * alphaMultiplier)

        //vertical
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        tessellator.draw()
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        tessellator.draw()
        GlStateManager.color(
            c.red / 255f * 0.8f,
            c.green / 255f * 0.8f,
            c.blue / 255f * 0.8f,
            c.alpha / 255f * alphaMultiplier
        )

        //x
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        tessellator.draw()
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        tessellator.draw()
        GlStateManager.color(
            c.red / 255f * 0.9f,
            c.green / 255f * 0.9f,
            c.blue / 255f * 0.9f,
            c.alpha / 255f * alphaMultiplier
        )
        //z
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
        tessellator.draw()
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
        tessellator.draw()
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    // TODO nea please merge with 'drawFilledBoundingBox'
    fun LorenzRenderWorldEvent.drawFilledBoundingBox_nea(
        aabb: AxisAlignedBB,
        c: Color,
        alphaMultiplier: Float = 1f,
        /**
         * If set to `true`, renders the box relative to the camera instead of relative to the world.
         * If set to `false`, will be relativized to [RenderUtils.getViewerPos].
         */
        renderRelativeToCamera: Boolean = false,
        drawVerticalBarriers: Boolean = true
    ) {
        drawFilledBoundingBox_nea(aabb, c, alphaMultiplier, renderRelativeToCamera, drawVerticalBarriers, partialTicks)
    }


    fun drawWireframeBoundingBox_nea(
        aabb: AxisAlignedBB,
        c: Color,
        partialTicks: Float,
    ) {
        GlStateManager.enableBlend()
        GlStateManager.disableLighting()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.disableTexture2D()
        GlStateManager.disableCull()
        val vp = getViewerPos(partialTicks)
        val effectiveAABB = AxisAlignedBB(
            aabb.minX - vp.x, aabb.minY - vp.y, aabb.minZ - vp.z,
            aabb.maxX - vp.x, aabb.maxY - vp.y, aabb.maxZ - vp.z,
        )
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        GlStateManager.color(c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f)
        // Bottom face
        worldRenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION)
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.minY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.minY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.maxZ).endVertex()
        tessellator.draw()

        // Top face
        worldRenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION)
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.maxY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.maxY, effectiveAABB.minZ).endVertex()
        tessellator.draw()


        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION)

        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.maxY, effectiveAABB.minZ).endVertex()

        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.maxY, effectiveAABB.maxZ).endVertex()

        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.minY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.minZ).endVertex()

        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.minY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.maxZ).endVertex()

        tessellator.draw()

        GlStateManager.enableTexture2D()
        GlStateManager.enableCull()
        GlStateManager.disableBlend()
    }


    fun drawFilledBoundingBox_nea(
        aabb: AxisAlignedBB,
        c: Color,
        alphaMultiplier: Float = 1f,
        /**
         * If set to `true`, renders the box relative to the camera instead of relative to the world.
         * If set to `false`, will be relativized to [RenderUtils.getViewerPos]. Setting this to `false` requires
         * specifying [partialTicks]]
         */
        renderRelativeToCamera: Boolean = true,
        drawVerticalBarriers: Boolean = true,
        partialTicks: Float = 0F,
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

        //vertical
        if (drawVerticalBarriers) {
            GlStateManager.color(c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f * alphaMultiplier)
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            worldRenderer.pos(effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.minZ).endVertex()
            worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.minY, effectiveAABB.minZ).endVertex()
            worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.minY, effectiveAABB.maxZ).endVertex()
            worldRenderer.pos(effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.maxZ).endVertex()
            tessellator.draw()
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            worldRenderer.pos(effectiveAABB.minX, effectiveAABB.maxY, effectiveAABB.maxZ).endVertex()
            worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.maxZ).endVertex()
            worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.minZ).endVertex()
            worldRenderer.pos(effectiveAABB.minX, effectiveAABB.maxY, effectiveAABB.minZ).endVertex()
            tessellator.draw()
        }
        GlStateManager.color(
            c.red / 255f * 0.8f,
            c.green / 255f * 0.8f,
            c.blue / 255f * 0.8f,
            c.alpha / 255f * alphaMultiplier
        )

        //x
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.maxY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.maxY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.minZ).endVertex()
        tessellator.draw()
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.minY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.minY, effectiveAABB.maxZ).endVertex()
        tessellator.draw()
        GlStateManager.color(
            c.red / 255f * 0.9f,
            c.green / 255f * 0.9f,
            c.blue / 255f * 0.9f,
            c.alpha / 255f * alphaMultiplier
        )
        //z
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.maxY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.minY, effectiveAABB.minZ).endVertex()
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.minZ).endVertex()
        tessellator.draw()
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.minY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.minY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.maxX, effectiveAABB.maxY, effectiveAABB.maxZ).endVertex()
        worldRenderer.pos(effectiveAABB.minX, effectiveAABB.maxY, effectiveAABB.maxZ).endVertex()
        tessellator.draw()
        GlStateManager.enableTexture2D()
        GlStateManager.enableCull()
        GlStateManager.disableBlend()
    }

    fun LorenzRenderWorldEvent.outlineTopFace(
        boundingBox: AxisAlignedBB,
        lineWidth: Int,
        colour: Color,
        depth: Boolean
    ) {
        val cornerOne = LorenzVec(boundingBox.minX, boundingBox.maxY, boundingBox.minZ)
        val cornerTwo = LorenzVec(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ)
        val cornerThree = LorenzVec(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)
        val cornerFour = LorenzVec(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ)
        this.draw3DLine(cornerOne, cornerTwo, colour, lineWidth, depth)
        this.draw3DLine(cornerTwo, cornerThree, colour, lineWidth, depth)
        this.draw3DLine(cornerThree, cornerFour, colour, lineWidth, depth)
        this.draw3DLine(cornerFour, cornerOne, colour, lineWidth, depth)
    }

    // TODO nea please merge with 'draw3DLine'
    fun LorenzRenderWorldEvent.draw3DLine_nea(
        p1: LorenzVec, p2: LorenzVec, color: Color, lineWidth: Int, depth: Boolean
    ) {
        GlStateManager.disableDepth()
        GlStateManager.disableCull()

        val render = Minecraft.getMinecraft().renderViewEntity
        val worldRenderer = Tessellator.getInstance().worldRenderer
        val realX = render.lastTickPosX + (render.posX - render.lastTickPosX) * partialTicks
        val realY = render.lastTickPosY + (render.posY - render.lastTickPosY) * partialTicks
        val realZ = render.lastTickPosZ + (render.posZ - render.lastTickPosZ) * partialTicks
        GlStateManager.pushMatrix()
        GlStateManager.translate(-realX, -realY, -realZ)
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GL11.glLineWidth(lineWidth.toFloat())
        if (!depth) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GlStateManager.depthMask(false)
        }
        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)
        worldRenderer.pos(p1.x, p1.y, p1.z).endVertex()
        worldRenderer.pos(p2.x, p2.y, p2.z).endVertex()
        Tessellator.getInstance().draw()
        GlStateManager.translate(realX, realY, realZ)
        if (!depth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GlStateManager.depthMask(true)
        }
        GlStateManager.disableBlend()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.popMatrix()
        GlStateManager.disableLighting()
        GlStateManager.enableDepth()
    }

    fun chromaColor(
        timeTillRepeat: Duration,
        offset: Float = 0f,
        saturation: Float = 1F,
        brightness: Float = 0.8F,
        timeOverride: Long = System.currentTimeMillis()
    ): Color {
        return Color(
            Color.HSBtoRGB(
                ((offset + timeOverride / timeTillRepeat.toDouble(DurationUnit.MILLISECONDS)) % 1).toFloat(),
                saturation,
                brightness
            )
        )
    }

    fun GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost.drawSlotText(
        xPos: Int,
        yPos: Int,
        text: String,
        scale: Float
    ) {
        val fontRenderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()

        GlStateManager.pushMatrix()
        GlStateManager.translate((xPos - fontRenderer.getStringWidth(text)).toFloat(), yPos.toFloat(), 0f)
        GlStateManager.scale(scale, scale, 1f)
        fontRenderer.drawStringWithShadow(text, 0f, 0f, 16777215)

        val reverseScale = 1 / scale

        GlStateManager.scale(reverseScale, reverseScale, 1f)
        GlStateManager.popMatrix()

        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }
}
