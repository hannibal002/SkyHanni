package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ColorUtils.component1
import at.hannibal2.skyhanni.utils.ColorUtils.component2
import at.hannibal2.skyhanni.utils.ColorUtils.component3
import at.hannibal2.skyhanni.utils.ColorUtils.component4
import at.hannibal2.skyhanni.utils.ItemBlink.checkBlinkItem
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.fractionOf
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import java.awt.Color
import java.text.DecimalFormat
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

/**
 * Some functions taken from NotEnoughUpdates
 */
@Suppress("UnusedParameter")
object GuiRenderUtils {

    private val fr: FontRenderer get() = Minecraft.getMinecraft().fontRendererObj

    private fun drawStringCentered(str: String?, x: Float, y: Float, shadow: Boolean, color: Int) {
        str ?: return
        val strLen = fr.getStringWidth(str)
        val x2 = x - strLen / 2f
        val y2 = y - fr.FONT_HEIGHT / 2f
        DrawContextUtils.drawContext.drawString(fr, str, x2.toInt(), y2.toInt(), color, shadow)
    }

    fun drawStringCentered(str: String?, x: Int, y: Int) {
        drawStringCentered(str, x.toFloat(), y.toFloat(), true, 0xffffff)
    }

    fun drawStringCenteredScaledMaxWidth(text: String, x: Float, y: Float, shadow: Boolean, length: Int, color: Int) {
        DrawContextUtils.pushMatrix()
        val strLength = fr.getStringWidth(text)
        val factor = min((length / strLength.toFloat()).toDouble(), 1.0).toFloat()
        DrawContextUtils.translate(x, y, 0f)
        DrawContextUtils.scale(factor, factor, 1f)
        drawString(text, -strLength / 2, -fr.FONT_HEIGHT / 2, color, shadow)
        DrawContextUtils.popMatrix()
    }

    fun drawString(str: String, x: Float, y: Float, color: Int = 0xffffff, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x.toInt(), y.toInt(), color, shadow)
    }

    fun drawString(str: String, x: Int, y: Int, color: Int = 0xffffff, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x, y, color, shadow)
    }

    private fun renderItemStack(item: ItemStack, x: Int, y: Int) {
        val itemRender = Minecraft.getMinecraft().renderItem
        RenderHelper.enableGUIStandardItemLighting()
        itemRender.zLevel = -145f
        itemRender.renderItemAndEffectIntoGUI(item, x, y)
        itemRender.zLevel = 0f
        RenderHelper.disableStandardItemLighting()
    }

    fun isPointInRect(x: Int, y: Int, left: Int, top: Int, width: Int, height: Int) =
        left <= x && x < left + width && top <= y && y < top + height

    fun getFarmingBar(
        label: String,
        tooltip: String,
        currentValue: Number,
        maxValue: Number,
        width: Int,
        textScale: Float = .7f,
    ): Renderable {
        val current = currentValue.toDouble().coerceAtLeast(0.0)
        val percent = current.fractionOf(maxValue)
        val scale = textScale.toDouble()
        return Renderable.hoverTips(
            Renderable.verticalContainer(
                listOf(
                    Renderable.string(label, scale = scale),
                    Renderable.fixedSizeLine(
                        listOf(
                            Renderable.string(
                                "§2${DecimalFormat("0.##").format(current)} / ${
                                    DecimalFormat(
                                        "0.##",
                                    ).format(maxValue)
                                }☘",
                                scale = scale, horizontalAlign = HorizontalAlignment.LEFT,
                            ),
                            Renderable.string(
                                "§2${(percent * 100).roundTo(1)}%",
                                scale = scale,
                                horizontalAlign = HorizontalAlignment.RIGHT,
                            ),
                        ),
                        width,
                    ),
                    Renderable.progressBar(percent, width = width),
                ),
            ),
            tooltip.split('\n').map { Renderable.string(it) },
        )
    }

    fun drawScaledRec(left: Int, top: Int, right: Int, bottom: Int, color: Int, inverseScale: Float) {
        drawRect(
            (left * inverseScale).toInt(),
            (top * inverseScale).toInt(),
            (right * inverseScale).toInt(),
            (bottom * inverseScale).toInt(),
            color,
        )
    }

    fun drawRect(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        DrawContextUtils.drawContext.fill(left, top, right, bottom, color)
    }

    fun renderItemAndBackground(item: ItemStack, x: Int, y: Int, color: Int) {
        renderItemStack(item, x, y)
        drawRect(x, y, x + 16, y + 16, color)
    }

    /** @Mojang */
    fun drawGradientRect(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        startColor: Int = -0xfeffff0,
        endColor: Int = -0xfeffff0,
        zLevel: Double = 0.0,
    ) {
        val (startAlpha, startRed, startGreen, startBlue) = Color(startColor)
        val (endAlpha, endRed, endGreen, endBlue) = Color(endColor)
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.shadeModel(7425)
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR)
        worldRenderer.pos(right.toDouble(), top.toDouble(), zLevel)
            .color(startRed, startGreen, startBlue, startAlpha).endVertex()
        worldRenderer.pos(left.toDouble(), top.toDouble(), zLevel)
            .color(startRed, startGreen, startBlue, startAlpha).endVertex()
        worldRenderer.pos(left.toDouble(), bottom.toDouble(), zLevel)
            .color(endRed, endGreen, endBlue, endAlpha).endVertex()
        worldRenderer.pos(right.toDouble(), bottom.toDouble(), zLevel)
            .color(endRed, endGreen, endBlue, endAlpha).endVertex()
        tessellator.draw()
        GlStateManager.shadeModel(7424)
        GlStateManager.disableBlend()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
    }

    fun drawTexturedRect(x: Float, y: Float) {
        with(ScaledResolution(Minecraft.getMinecraft())) {
            drawTexturedRect(x, y, scaledWidth.toFloat(), scaledHeight.toFloat(), filter = GL11.GL_NEAREST)
        }
    }

    fun drawTexturedRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        uMin: Float = 0f,
        uMax: Float = 1f,
        vMin: Float = 0f,
        vMax: Float = 1f,
        filter: Int = GL11.GL_NEAREST,
    ) {
        drawTexturedRect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), uMin, uMax, vMin, vMax, filter)
    }

    // Taken from NEU
    private fun drawTexturedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        uMin: Float = 0f,
        uMax: Float = 1f,
        vMin: Float = 0f,
        vMax: Float = 1f,
        filter: Int = GL11.GL_NEAREST,
    ) {
        GlStateManager.enableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter)

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0).tex(uMin.toDouble(), vMax.toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0).tex(uMax.toDouble(), vMax.toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), y.toDouble(), 0.0).tex(uMax.toDouble(), vMin.toDouble()).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), 0.0).tex(uMin.toDouble(), vMin.toDouble()).endVertex()
        tessellator.draw()

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        GlStateManager.disableBlend()
    }

    fun ItemStack.renderOnScreen(
        x: Float,
        y: Float,
        scaleMultiplier: Double = NeuItems.ITEM_FONT_SIZE,
        rescaleSkulls: Boolean = true,
    ) {
        val item = checkBlinkItem()
        val isSkull = rescaleSkulls && item.item === Items.skull

        val baseScale = (if (isSkull) 4f / 3f else 1f)
        val finalScale = baseScale * scaleMultiplier

        val translateX: Float
        val translateY: Float
        if (isSkull) {
            val skullDiff = ((scaleMultiplier) * 2.5).toFloat()
            translateX = x - skullDiff
            translateY = y - skullDiff
        } else {
            translateX = x
            translateY = y
        }

        GlStateManager.pushMatrix()

        GlStateManager.translate(translateX, translateY, -19f)
        GlStateManager.scale(finalScale, finalScale, 0.2)
        GL11.glNormal3f(0f, 0f, 1f / 0.2f) // Compensate for z scaling

        RenderHelper.enableGUIStandardItemLighting()

        AdjustStandardItemLighting.adjust() // Compensate for z scaling

        try {
            Minecraft.getMinecraft().renderItem.renderItemIntoGUI(item, 0, 0)
        } catch (e: Exception) {
            if (lastWarn.passedSince() > 1.seconds) {
                lastWarn = SimpleTimeMark.now()
                println(" ")
                println("item: $item")
                println("name: ${item.displayName}")
                println("getInternalNameOrNull: ${item.getInternalNameOrNull()}")
                println(" ")
                ChatUtils.debug("rendering an item has failed.")
            }
        }
        RenderHelper.disableStandardItemLighting()

        GlStateManager.popMatrix()
    }

    private var lastWarn = SimpleTimeMark.farPast()

    private object AdjustStandardItemLighting {

        private const val lightScaling = 2.47f // Adjust as needed
        private const val g = 0.6f // Original Value taken from RenderHelper
        private const val lightIntensity = lightScaling * g
        private val itemLightBuffer = GLAllocation.createDirectFloatBuffer(16)

        init {
            itemLightBuffer.clear()
            itemLightBuffer.put(lightIntensity).put(lightIntensity).put(lightIntensity).put(1.0f)
            itemLightBuffer.flip()
        }

        fun adjust() {
            GL11.glLight(16384, 4609, itemLightBuffer)
            GL11.glLight(16385, 4609, itemLightBuffer)
        }
    }
}
