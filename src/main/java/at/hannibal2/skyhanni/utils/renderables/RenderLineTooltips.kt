package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.item.ItemStack
import java.awt.Color

object RenderLineTooltips {

    fun drawHoveringText(
        posX: Int, posY: Int, tips: List<String?>, stack: ItemStack? = null,
        mouseX: Int = Utils.getMouseX(),
        mouseY: Int = Utils.getMouseY(),
    ) {
        if (tips.isNotEmpty()) {
            var textLines = tips
            val x = mouseX + 12 - posX
            val y = mouseY - 10 - posY
            val color: Char = stack?.getLore()?.lastOrNull()?.take(4)?.get(1)
                ?: Utils.getPrimaryColourCode(textLines[0])
            val colourInt = Minecraft.getMinecraft().fontRendererObj.getColorCode(color)
            val borderColorStart = Color(colourInt).darker().rgb and 0x00FFFFFF or (200 shl 24)
            val font = Minecraft.getMinecraft().fontRendererObj
            val scaled = ScaledResolution(Minecraft.getMinecraft())
            GlStateManager.disableRescaleNormal()
            RenderHelper.disableStandardItemLighting()
            GlStateManager.disableLighting()
            GlStateManager.enableDepth()
            var tooltipTextWidth = 0
            for (textLine in textLines) {
                val textLineWidth = font.getStringWidth(textLine)
                if (textLineWidth > tooltipTextWidth) {
                    tooltipTextWidth = textLineWidth
                }
            }
            var needsWrap = false
            var titleLinesCount = 1
            var tooltipX = x
            if (tooltipX + tooltipTextWidth + 4 > scaled.scaledWidth) {
                tooltipX = x - 16 - tooltipTextWidth
                if (tooltipX < 4) {
                    tooltipTextWidth = if (x > scaled.scaledWidth / 2) {
                        x - 12 - 8
                    } else {
                        scaled.scaledWidth - 16 - x
                    }
                    needsWrap = true
                }
            }
            if (needsWrap) {
                var wrappedTooltipWidth = 0
                val wrappedTextLines: MutableList<String?> = ArrayList()
                for ((i, textLine) in textLines.withIndex()) {
                    val wrappedLine = font.listFormattedStringToWidth(textLine, tooltipTextWidth)
                    if (i == 0) {
                        titleLinesCount = wrappedLine.size
                    }
                    for (line in wrappedLine) {
                        val lineWidth = font.getStringWidth(line)
                        if (lineWidth > wrappedTooltipWidth) {
                            wrappedTooltipWidth = lineWidth
                        }
                        wrappedTextLines.add(line)
                    }
                }
                tooltipTextWidth = wrappedTooltipWidth
                textLines = wrappedTextLines.toList()
                tooltipX = if (x > scaled.scaledWidth / 2) {
                    x - 16 - tooltipTextWidth
                } else {
                    x + 12
                }
            }
            var tooltipY = y - 12
            var tooltipHeight = 8
            if (textLines.size > 1) {
                tooltipHeight += (textLines.size - 1) * 10
                if (textLines.size > titleLinesCount) {
                    tooltipHeight += 2
                }
            }

            if (tooltipY + tooltipHeight + 6 > scaled.scaledHeight) {
                tooltipY = scaled.scaledHeight - tooltipHeight - 6
            }
            val zLevel = 300
            val backgroundColor = -0xfeffff0
            drawGradientRect(
                zLevel,
                tooltipX - 3,
                tooltipY - 4,
                tooltipX + tooltipTextWidth + 3,
                tooltipY - 3,
                backgroundColor,
                backgroundColor
            )
            drawGradientRect(
                zLevel,
                tooltipX - 3,
                tooltipY + tooltipHeight + 3,
                tooltipX + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 4,
                backgroundColor,
                backgroundColor
            )
            drawGradientRect(
                zLevel,
                tooltipX - 3,
                tooltipY - 3,
                tooltipX + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 3,
                backgroundColor,
                backgroundColor
            )
            drawGradientRect(
                zLevel,
                tooltipX - 4,
                tooltipY - 3,
                tooltipX - 3,
                tooltipY + tooltipHeight + 3,
                backgroundColor,
                backgroundColor
            )
            drawGradientRect(
                zLevel,
                tooltipX + tooltipTextWidth + 3,
                tooltipY - 3,
                tooltipX + tooltipTextWidth + 4,
                tooltipY + tooltipHeight + 3,
                backgroundColor,
                backgroundColor
            )
            val borderColorEnd = borderColorStart and 0xFEFEFE shr 1 or (borderColorStart and -0x1000000)
            drawGradientRect(
                zLevel,
                tooltipX - 3,
                tooltipY - 3 + 1,
                tooltipX - 3 + 1,
                tooltipY + tooltipHeight + 3 - 1,
                borderColorStart,
                borderColorEnd
            )
            drawGradientRect(
                zLevel,
                tooltipX + tooltipTextWidth + 2,
                tooltipY - 3 + 1,
                tooltipX + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 3 - 1,
                borderColorStart,
                borderColorEnd
            )
            drawGradientRect(
                zLevel,
                tooltipX - 3,
                tooltipY - 3,
                tooltipX + tooltipTextWidth + 3,
                tooltipY - 3 + 1,
                borderColorStart,
                borderColorStart
            )
            drawGradientRect(
                zLevel,
                tooltipX - 3,
                tooltipY + tooltipHeight + 2,
                tooltipX + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 3,
                borderColorEnd,
                borderColorEnd
            )
            GlStateManager.disableDepth()
            for ((lineNumber, line) in textLines.withIndex()) {
                val line = textLines[lineNumber]
                font.drawStringWithShadow(line, 1f + tooltipX.toFloat(), 1f + tooltipY.toFloat(), -1)
                if (lineNumber + 1 == titleLinesCount) {
                    tooltipY += 2
                }
                tooltipY += 10
            }
            GlStateManager.enableLighting()
            GlStateManager.enableDepth()
            RenderHelper.enableStandardItemLighting()
            GlStateManager.enableRescaleNormal()
        }
        GlStateManager.disableLighting()
    }

    fun drawHoveringText(
        posX: Int, posY: Int,
        tips: List<Renderable?>,
        stack: ItemStack? = null,
        borderColor: LorenzColor? = null,
        mouseX: Int = Utils.getMouseX(),
        mouseY: Int = Utils.getMouseY(),
    ) {
        if (tips.isNotEmpty()) {
            val x = mouseX + 12 - posX
            val y = mouseY - 10 - posY
            val color: Char = borderColor?.chatColorCode ?: stack?.getLore()?.lastOrNull()?.take(4)?.get(1)
            ?: 'f'
            val colourInt = Minecraft.getMinecraft().fontRendererObj.getColorCode(color)
            val borderColorStart = Color(colourInt).darker().rgb and 0x00FFFFFF or (200 shl 24)
            val scaled = ScaledResolution(Minecraft.getMinecraft())
            GlStateManager.disableRescaleNormal()
            RenderHelper.disableStandardItemLighting()
            GlStateManager.enableDepth()
            val tooltipTextWidth = tips.maxOf { it?.width ?: 0 }
            val tooltipHeight = tips.sumOf { it?.height ?: 0 }
            val tooltipY = if (y - 6 + tooltipHeight > scaled.scaledHeight) scaled.scaledHeight - tooltipHeight - 6 else y - 12

            val zLevel = 300
            val backgroundColor = -0xfeffff0
            drawGradientRect(
                zLevel,
                x - 3,
                tooltipY - 4,
                x + tooltipTextWidth + 3,
                tooltipY - 3,
                backgroundColor,
                backgroundColor
            )
            drawGradientRect(
                zLevel,
                x - 3,
                tooltipY + tooltipHeight + 3,
                x + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 4,
                backgroundColor,
                backgroundColor
            )
            drawGradientRect(
                zLevel,
                x - 3,
                tooltipY - 3,
                x + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 3,
                backgroundColor,
                backgroundColor
            )
            drawGradientRect(
                zLevel,
                x - 4,
                tooltipY - 3,
                x - 3,
                tooltipY + tooltipHeight + 3,
                backgroundColor,
                backgroundColor
            )
            drawGradientRect(
                zLevel,
                x + tooltipTextWidth + 3,
                tooltipY - 3,
                x + tooltipTextWidth + 4,
                tooltipY + tooltipHeight + 3,
                backgroundColor,
                backgroundColor
            )
            val borderColorEnd = borderColorStart and 0xFEFEFE shr 1 or (borderColorStart and -0x1000000)
            drawGradientRect(
                zLevel,
                x - 3,
                tooltipY - 3 + 1,
                x - 3 + 1,
                tooltipY + tooltipHeight + 3 - 1,
                borderColorStart,
                borderColorEnd
            )
            drawGradientRect(
                zLevel,
                x + tooltipTextWidth + 2,
                tooltipY - 3 + 1,
                x + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 3 - 1,
                borderColorStart,
                borderColorEnd
            )
            drawGradientRect(
                zLevel,
                x - 3,
                tooltipY - 3,
                x + tooltipTextWidth + 3,
                tooltipY - 3 + 1,
                borderColorStart,
                borderColorStart
            )
            drawGradientRect(
                zLevel,
                x - 3,
                tooltipY + tooltipHeight + 2,
                x + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 3,
                borderColorEnd,
                borderColorEnd
            )
            GlStateManager.disableDepth()
            GlStateManager.translate(x.toFloat(), tooltipY.toFloat(), 0f)
            var yTranslateSum = 0
            for (line in tips) {
                // TODO
                line?.render(x, tooltipY)
                val y = line?.height ?: 0
                GlStateManager.translate(0f, y.toFloat(), 0f)
                yTranslateSum += y
            }
            GlStateManager.translate(-x.toFloat(), -tooltipY.toFloat() + yTranslateSum.toFloat(), 0f)
            GlStateManager.enableLighting()
            GlStateManager.enableDepth()
            RenderHelper.enableStandardItemLighting()
            GlStateManager.enableRescaleNormal()
        }
        GlStateManager.disableLighting()
    }

    private fun drawGradientRect(
        zLevel: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        startColor: Int,
        endColor: Int,
    ) {
        val startAlpha = (startColor shr 24 and 255).toFloat() / 255.0f
        val startRed = (startColor shr 16 and 255).toFloat() / 255.0f
        val startGreen = (startColor shr 8 and 255).toFloat() / 255.0f
        val startBlue = (startColor and 255).toFloat() / 255.0f
        val endAlpha = (endColor shr 24 and 255).toFloat() / 255.0f
        val endRed = (endColor shr 16 and 255).toFloat() / 255.0f
        val endGreen = (endColor shr 8 and 255).toFloat() / 255.0f
        val endBlue = (endColor and 255).toFloat() / 255.0f
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.shadeModel(7425)
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR)
        worldrenderer.pos(right.toDouble(), top.toDouble(), zLevel.toDouble())
            .color(startRed, startGreen, startBlue, startAlpha).endVertex()
        worldrenderer.pos(left.toDouble(), top.toDouble(), zLevel.toDouble())
            .color(startRed, startGreen, startBlue, startAlpha).endVertex()
        worldrenderer.pos(left.toDouble(), bottom.toDouble(), zLevel.toDouble())
            .color(endRed, endGreen, endBlue, endAlpha).endVertex()
        worldrenderer.pos(right.toDouble(), bottom.toDouble(), zLevel.toDouble())
            .color(endRed, endGreen, endBlue, endAlpha).endVertex()
        tessellator.draw()
        GlStateManager.shadeModel(7424)
        GlStateManager.disableBlend()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
    }
}
