package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.features.skillprogress.SkillProgressBarConfig
import at.hannibal2.skyhanni.features.chroma.ChromaShaderManager
import at.hannibal2.skyhanni.features.chroma.ChromaType
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Some functions taken from NotEnoughUpdates
 */
object GuiRenderUtils {

    fun drawStringCentered(str: String?, fr: FontRenderer, x: Float, y: Float, shadow: Boolean, colour: Int) {
        val strLen = fr.getStringWidth(str)
        val x2 = x - strLen / 2f
        val y2 = y - fr.FONT_HEIGHT / 2f
        GL11.glTranslatef(x2, y2, 0f)
        fr.drawString(str, 0f, 0f, colour, shadow)
        GL11.glTranslatef(-x2, -y2, 0f)
    }

    fun drawString(str: String, x: Float, y: Float) {
        Minecraft.getMinecraft().fontRendererObj.drawString(str, x, y, 0xffffff, true)
    }

    fun drawString(str: String, x: Int, y: Int) {
        Minecraft.getMinecraft().fontRendererObj.drawString(str, x.toFloat(), y.toFloat(), 0xffffff, true)
    }

    fun drawTwoLineString(str: String, x: Float, y: Float) {
        val desiredSplitIndex = str.length / 2
        var splitIndex = -1
        var lastColorCode = ""

        for (i in desiredSplitIndex downTo 0) {
            if (str[i] == ' ') {
                splitIndex = i
                break
            }
        }

        if (splitIndex == -1) {
            splitIndex = desiredSplitIndex
        }
        for (i in 0 until desiredSplitIndex) {
            if (str[i] == '§' && i + 1 < str.length) {
                lastColorCode = str.substring(i, i + 2)
            }
        }

        val firstString = str.substring(0, splitIndex).trim()
        val secondString = lastColorCode + str.substring(splitIndex).trim()

        Minecraft.getMinecraft().fontRendererObj.drawString(firstString, x, y - 5, 0xffffff, true)
        Minecraft.getMinecraft().fontRendererObj.drawString(secondString, x, y + 5, 0xffffff, true)
    }

    fun drawStringCentered(str: String?, x: Int, y: Int) {
        drawStringCentered(
            str,
            Minecraft.getMinecraft().fontRendererObj,
            x.toFloat(),
            y.toFloat(),
            true,
            0xffffff
        )
    }

    fun drawStringCentered(str: String?, x: Float, y: Float) {
        drawStringCentered(str, x.toInt(), y.toInt())
    }

    fun renderItemStack(item: ItemStack, x: Int, y: Int) {
        val itemRender = Minecraft.getMinecraft().renderItem
        RenderHelper.enableGUIStandardItemLighting()
        itemRender.zLevel = -145f
        itemRender.renderItemAndEffectIntoGUI(item, x, y)
        itemRender.zLevel = 0f
        RenderHelper.disableStandardItemLighting()
    }

    // Code taken and edited from NEU
    private fun drawTooltip(
        textLines: List<String>,
        mouseX: Int,
        mouseY: Int,
        screenHeight: Int,
        fr: FontRenderer,
    ) {
        if (textLines.isNotEmpty()) {
            val borderColor = StringUtils.getColor(textLines[0], 0x505000FF)

            GlStateManager.disableRescaleNormal()
            RenderHelper.disableStandardItemLighting()
            GlStateManager.disableLighting()
            GlStateManager.enableDepth()
            var tooltipTextWidth = 0

            for (textLine in textLines) {
                val textLineWidth: Int = fr.getStringWidth(textLine)
                if (textLineWidth > tooltipTextWidth) {
                    tooltipTextWidth = textLineWidth
                }
            }

            val tooltipX = mouseX + 12
            var tooltipY = mouseY - 12
            var tooltipHeight = 8

            if (textLines.size > 1) tooltipHeight += (textLines.size - 1) * 10 + 2
            GlStateManager.translate(0f, 0f, 100f)
            if (tooltipY + tooltipHeight + 6 > screenHeight) tooltipY = screenHeight - tooltipHeight - 6
            // main background
            GuiScreen.drawRect(
                tooltipX - 3, tooltipY - 3,
                tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, -0xfeffff0
            )

            // borders
            GuiScreen.drawRect(
                tooltipX - 3, tooltipY - 3 + 1,
                tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, borderColor

            )

            GuiScreen.drawRect(
                tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1,
                tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, borderColor
            )

            GuiScreen.drawRect(
                tooltipX - 3, tooltipY - 3,
                tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, borderColor
            )

            GuiScreen.drawRect(
                tooltipX - 3, tooltipY + tooltipHeight + 2,
                tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderColor
            )
            GlStateManager.translate(0f, 0f, -100f)
            GlStateManager.disableDepth()

            for (line in textLines) {
                fr.drawString(line, tooltipX.toFloat(), tooltipY.toFloat(), 0xffffff, true)

                tooltipY += if (line == textLines[0]) 12 else 10
            }

            GlStateManager.enableDepth()
            GlStateManager.enableLighting()
            GlStateManager.enableRescaleNormal()
            RenderHelper.enableStandardItemLighting()
        }
        GlStateManager.disableLighting()
    }

    fun drawTooltip(textLines: List<String>, mouseX: Int, mouseY: Int, screenHeight: Int) {
        drawTooltip(textLines, mouseX, mouseY, screenHeight, Minecraft.getMinecraft().fontRendererObj)
    }

    fun isPointInRect(x: Int, y: Int, left: Int, top: Int, width: Int, height: Int) =
        left <= x && x < left + width && top <= y && y < top + height

    fun drawProgressBar(x: Int, y: Int, barWidth: Int, progress: Float) {
        GuiScreen.drawRect(x, y, x + barWidth, y + 6, 0xFF43464B.toInt())
        val width = barWidth * progress
        GuiScreen.drawRect(x + 1, y + 1, (x + width).toInt() + 1, y + 5, 0xFF00FF00.toInt())
        if (progress != 1f) GuiScreen.drawRect(
            (x + width).toInt() + 1,
            y + 1,
            x + barWidth - 1,
            y + 5,
            0xFF013220.toInt()
        )
    }

    fun renderItemAndTip(
        list: MutableList<String>,
        item: ItemStack?,
        x: Int,
        y: Int,
        mouseX: Int,
        mouseY: Int,
        color: Int = 0xFF43464B.toInt(),
    ) {
        GuiScreen.drawRect(x, y, x + 16, y + 16, color)
        if (item != null) {
            renderItemStack(item, x, y)
            if (isPointInRect(mouseX, mouseY, x, y, 16, 16)) {
                val tt: List<String> = item.getTooltip(Minecraft.getMinecraft().thePlayer, false)
                list.addAll(tt)
            }
        }
    }

    fun renderItemAndTip(
        list: MutableList<String>,
        item: ItemStack?,
        x: Float,
        y: Float,
        mouseX: Float,
        mouseY: Float,
        color: Int = 0xFF43464B.toInt(),
    ) {
        renderItemAndTip(list, item, x.toInt(), y.toInt(), mouseX.toInt(), mouseY.toInt(), color)
    }

    // assuming 70% font size
    fun drawFarmingBar(
        label: String,
        tooltip: String,
        currentValue: Number,
        maxValue: Number,
        xPos: Int,
        yPos: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        output: MutableList<String>,
        textScale: Float = .7f,
    ) {
        var currentVal = currentValue.toDouble()
        currentVal = if (currentVal < 0) 0.0 else currentVal

        var barProgress = currentVal / maxValue.toFloat()
        if (maxValue == 0) barProgress = 1.0
        barProgress = when {
            barProgress > 1 -> 1.0
            barProgress < 0 -> 0.0
            else -> barProgress
        }

        val filledWidth = (width * barProgress).toInt()
        val current = DecimalFormat("0.##").format(currentVal)
        val progressPercentage = (barProgress * 10000).roundToInt() / 100
        val inverseScale = 1 / textScale
        val textWidth: Int = Minecraft.getMinecraft().fontRendererObj.getStringWidth("$progressPercentage%")
        val barColor = barColorGradient(barProgress)

        GlStateManager.scale(textScale, textScale, 1f)
        drawString(label, xPos * inverseScale, yPos * inverseScale)
        drawString(
            "§2$current / ${DecimalFormat("0.##").format(maxValue)}☘",
            xPos * inverseScale,
            (yPos + 8) * inverseScale
        )
        drawString(
            "§2$progressPercentage%",
            (xPos + width - textWidth * textScale) * inverseScale,
            (yPos + 8) * inverseScale
        )
        GlStateManager.scale(inverseScale, inverseScale, 1f)

        GuiScreen.drawRect(xPos, yPos + 16, xPos + width, yPos + 20, 0xFF43464B.toInt())
        GuiScreen.drawRect(xPos + 1, yPos + 17, xPos + width - 1, yPos + 19, barColor.darkenColor())
        GuiScreen.drawRect(
            xPos + 1, yPos + 17,
            if (filledWidth < 2) xPos + 1 else xPos + filledWidth - 1, yPos + 19, barColor
        )

        if (tooltip != "" && isPointInRect(mouseX, mouseY, xPos - 2, yPos - 2, width + 4, 20 + 4)) {
            val split = tooltip.split("\n")
            for (line in split) {
                output.add(line)
            }
        }
    }

    private fun barColorGradient(double: Double): Int {
        var newDouble = (double - .5) * 2
        if (newDouble < 0) newDouble = 0.0
        return Color((255 * (1 - newDouble)).toInt(), (255 * newDouble).toInt(), 0).rgb
    }

    fun Int.darkenColor(): Int {
        val color = Color(this)
        return Color(color.red / 5, color.green / 5, color.blue / 5).rgb
    }

    fun drawScaledRec(left: Int, top: Int, right: Int, bottom: Int, colour: Int, inverseScale: Float) {
        GuiScreen.drawRect(
            (left * inverseScale).toInt(), (top * inverseScale).toInt(),
            (right * inverseScale).toInt(), (bottom * inverseScale).toInt(), colour
        )
    }

    fun renderItemAndBackground(item: ItemStack, x: Int, y: Int, colour: Int) {
        renderItemStack(item, x, y)
        GuiScreen.drawRect(x, y, x + 16, y + 16, colour)
    }

    // Taken and edited from NEU <- it's broken
    fun renderTexturedBar(x: Float, y: Float, xSize: Float, completed: Float, color: Color, useChroma: Boolean, texture: SkillProgressBarConfig.TexturedBar.UsedTexture, height: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)
        val w = xSize.toInt()
        val w_2 = w / 2
        val k = min(w.toDouble(), ceil((completed * w).toDouble())).toInt()
        val vanilla = texture == SkillProgressBarConfig.TexturedBar.UsedTexture.MATCH_PACK
        val vMinEmpty = if (vanilla) 64 / 256f else 0f
        val vMaxEmpty = if (vanilla) 69 / 256f else .5f
        val vMinFilled = if (vanilla) 69 / 256f else .5f
        val vMaxFilled = if (vanilla) 74 / 256f else 1f

        if (useChroma) {
            ChromaShaderManager.begin(ChromaType.TEXTURED)
            GlStateManager.color(Color.LIGHT_GRAY.darker().red / 255f, Color.LIGHT_GRAY.darker().green / 255f, Color.LIGHT_GRAY.darker().blue / 255f, 1f)
        } else {
            GlStateManager.color(color.darker().red / 255f, color.darker().green / 255f, color.darker().blue / 255f, 1f)
        }

        Utils.drawTexturedRect(x, y, w_2.toFloat(), height, 0f, w_2 / xSize, vMinEmpty, vMaxEmpty, GL11.GL_NEAREST)
        Utils.drawTexturedRect(x + w_2, y, w_2.toFloat(), height, 1 - w_2 / xSize, 1f, vMinEmpty, vMaxEmpty, GL11.GL_NEAREST)

        if (useChroma) {
            GlStateManager.color(Color.WHITE.red / 255f, Color.WHITE.green / 255f, Color.WHITE.blue / 255f, 1f)
        } else {
            GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
        }

         if (k > 0) {
            Utils.drawTexturedRect(x, y, w_2.coerceAtMost(k).toFloat(), height, 0f, w_2.toDouble().coerceAtMost(k.toDouble() / xSize).toFloat(), vMinFilled, vMaxFilled, GL11.GL_NEAREST)
            if (completed > 0.5f) {
                Utils.drawTexturedRect(x + w_2, y, (k - w_2).toFloat(), height, 1 - w_2 / xSize, 1 + (k - w) / xSize, vMinFilled, vMaxFilled, GL11.GL_NEAREST)
            }
        }
        if (useChroma) {
            ChromaShaderManager.end()
        }
        GlStateManager.popMatrix()
    }
}
