package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
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
        posX: Int, posY: Int,
        tips: List<Renderable>,
        stack: ItemStack? = null,
        borderColor: LorenzColor? = null,
        snapsToTopIfToLong: Boolean = true,
        mouseX: Int = Utils.getMouseX(),
        mouseY: Int = Utils.getMouseY(),
    ) {
        if (tips.isEmpty()) return

        val (xTranslate, yTranslate, _) = RenderUtils.absoluteTranslation

        val x = mouseX - posX + 12
        val y = mouseY - posY - if (tips.size > 1) 2 else -7
        val color: Char = borderColor?.chatColorCode ?: stack?.getLore()?.lastOrNull()?.take(4)?.get(1)
        ?: 'f'
        val colourInt = Minecraft.getMinecraft().fontRendererObj.getColorCode(color)
        val borderColorStart = Color(colourInt).darker().rgb and 0x00FFFFFF or (200 shl 24)
        val scaled = ScaledResolution(Minecraft.getMinecraft())

        val tooltipTextWidth = tips.maxOf { it.width }
        val tooltipHeight = tips.sumOf { it.height }

        val tooltipY = when {
            y + yTranslate < 16 -> -yTranslate + 4 // Limit Top
            y + yTranslate + tooltipHeight > scaled.scaledHeight -> {
                if (snapsToTopIfToLong && tooltipHeight + 8 > scaled.scaledHeight)
                    -yTranslate + 4 // Snap to Top if to Long
                else
                    scaled.scaledHeight - tooltipHeight - 4 - yTranslate // Limit Bottom
            }

            else -> {
                y - 12 // normal
            }
        }
        val tooltipX = if (x + tooltipTextWidth + 4 + xTranslate > scaled.scaledWidth) {
            scaled.scaledWidth - tooltipTextWidth - 4 - xTranslate // Limit Right
        } else {
            x // normal
        }

        GlStateManager.disableRescaleNormal()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.enableDepth()

        val zLevel = 300f
        GlStateManager.translate(tooltipX.toFloat(), tooltipY.toFloat(), zLevel)

        drawGradientRect(
            left = -3,
            top = -4,
            right = tooltipTextWidth + 3,
            bottom = -3,
        )
        drawGradientRect(
            left = -3,
            top = tooltipHeight + 3,
            right = tooltipTextWidth + 3,
            bottom = tooltipHeight + 4,
        )
        drawGradientRect(
            left = -3,
            top = -3,
            right = tooltipTextWidth + 3,
            bottom = tooltipHeight + 3,
        )
        drawGradientRect(
            left = -4,
            top = -3,
            right = -3,
            bottom = tooltipHeight + 3,
        )
        drawGradientRect(
            left = tooltipTextWidth + 3,
            top = -3,
            right = tooltipTextWidth + 4,
            bottom = tooltipHeight + 3,
        )
        val borderColorEnd = borderColorStart and 0xFEFEFE shr 1 or (borderColorStart and -0x1000000)
        drawGradientRect(
            left = -3,
            top = -3 + 1,
            right = -3 + 1,
            bottom = tooltipHeight + 3 - 1,
            startColor = borderColorStart,
            endColor = borderColorEnd
        )
        drawGradientRect(
            left = tooltipTextWidth + 2,
            top = -3 + 1,
            right = tooltipTextWidth + 3,
            bottom = tooltipHeight + 3 - 1,
            startColor = borderColorStart,
            endColor = borderColorEnd
        )
        drawGradientRect(
            left = -3,
            top = -3,
            right = tooltipTextWidth + 3,
            bottom = -3 + 1,
            startColor = borderColorStart,
            endColor = borderColorStart
        )
        drawGradientRect(
            left = -3,
            top = tooltipHeight + 2,
            right = tooltipTextWidth + 3,
            bottom = tooltipHeight + 3,
            startColor = borderColorEnd,
            endColor = borderColorEnd
        )
        GlStateManager.disableDepth()
        GlStateManager.translate(0f, 0f, -zLevel)

        var yTranslateSum = 0
        for (line in tips) {
            line.renderXAligned(tooltipX, tooltipY, tooltipTextWidth)
            val yShift = line.height
            GlStateManager.translate(0f, yShift.toFloat(), 0f)
            yTranslateSum += yShift
        }

        GlStateManager.translate(-tooltipX.toFloat(), -tooltipY.toFloat() + yTranslateSum.toFloat(), 0f)
        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
        RenderHelper.enableStandardItemLighting()
        GlStateManager.enableRescaleNormal()
        GlStateManager.disableLighting()
    }

    private fun drawGradientRect(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        startColor: Int = -0xfeffff0,
        endColor: Int = -0xfeffff0,
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
        worldrenderer.pos(right.toDouble(), top.toDouble(), 0.0)
            .color(startRed, startGreen, startBlue, startAlpha).endVertex()
        worldrenderer.pos(left.toDouble(), top.toDouble(), 0.0)
            .color(startRed, startGreen, startBlue, startAlpha).endVertex()
        worldrenderer.pos(left.toDouble(), bottom.toDouble(), 0.0)
            .color(endRed, endGreen, endBlue, endAlpha).endVertex()
        worldrenderer.pos(right.toDouble(), bottom.toDouble(), 0.0)
            .color(endRed, endGreen, endBlue, endAlpha).endVertex()
        tessellator.draw()
        GlStateManager.shadeModel(7424)
        GlStateManager.disableBlend()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
    }
}
