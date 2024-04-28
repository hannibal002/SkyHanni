package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import io.github.moulberry.notenoughupdates.util.Utils
import java.awt.Color
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.ItemStack

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

        RenderUtils.drawGradientRect(
            left = -3,
            top = -4,
            right = tooltipTextWidth + 3,
            bottom = -3,
        )
        RenderUtils.drawGradientRect(
            left = -3,
            top = tooltipHeight + 3,
            right = tooltipTextWidth + 3,
            bottom = tooltipHeight + 4,
        )
        RenderUtils.drawGradientRect(
            left = -3,
            top = -3,
            right = tooltipTextWidth + 3,
            bottom = tooltipHeight + 3,
        )
        RenderUtils.drawGradientRect(
            left = -4,
            top = -3,
            right = -3,
            bottom = tooltipHeight + 3,
        )
        RenderUtils.drawGradientRect(
            left = tooltipTextWidth + 3,
            top = -3,
            right = tooltipTextWidth + 4,
            bottom = tooltipHeight + 3,
        )
        val borderColorEnd = borderColorStart and 0xFEFEFE shr 1 or (borderColorStart and -0x1000000)
        RenderUtils.drawGradientRect(
            left = -3,
            top = -3 + 1,
            right = -3 + 1,
            bottom = tooltipHeight + 3 - 1,
            startColor = borderColorStart,
            endColor = borderColorEnd
        )
        RenderUtils.drawGradientRect(
            left = tooltipTextWidth + 2,
            top = -3 + 1,
            right = tooltipTextWidth + 3,
            bottom = tooltipHeight + 3 - 1,
            startColor = borderColorStart,
            endColor = borderColorEnd
        )
        RenderUtils.drawGradientRect(
            left = -3,
            top = -3,
            right = tooltipTextWidth + 3,
            bottom = -3 + 1,
            startColor = borderColorStart,
            endColor = borderColorStart
        )
        RenderUtils.drawGradientRect(
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
}
