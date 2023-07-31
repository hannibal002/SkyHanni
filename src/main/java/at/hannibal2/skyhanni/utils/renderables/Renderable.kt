package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCharRarity
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarity
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.NEUItems.renderOnScreen
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapper
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.item.ItemStack
import org.lwjgl.input.Mouse
import java.awt.Color
import kotlin.math.max

interface Renderable {
    val width: Int
    val height: Int
    fun isHovered(posX: Int, posY: Int) =
        Utils.getMouseX() in (posX..posX + width)
                && Utils.getMouseY() in (posY..posY + height) // TODO: adjust for variable height?

    /**
     * N.B.: the offset is absolute, not relative to the position and shouldn't be used for rendering
     * (the GL matrix stack should already be pre transformed)
     */
    fun render(posX: Int, posY: Int)

    companion object {
        val logger = LorenzLogger("debug/renderable")
        val list = mutableMapOf<Pair<Int, Int>, List<Int>>()

        fun fromAny(any: Any?, itemScale: Double = 1.0): Renderable? = when (any) {
            null -> placeholder(12)
            is Renderable -> any
            is String -> string(any)
            is ItemStack -> itemStack(any, itemScale)
            else -> null
        }

        fun link(text: String, bypassChecks: Boolean = false, onClick: () -> Unit): Renderable = link(string(text), onClick, bypassChecks = bypassChecks) { true }
        fun optionalLink(text: String, onClick: () -> Unit, bypassChecks: Boolean = false, condition: () -> Boolean = { true }): Renderable =
            link(string(text), onClick, bypassChecks, condition)

        fun link(renderable: Renderable, onClick: () -> Unit, bypassChecks: Boolean = false, condition: () -> Boolean = { true }): Renderable {
            return clickable(hoverable(underlined(renderable), renderable, bypassChecks, condition = condition), onClick, 0, bypassChecks, condition)
        }

        fun clickAndHover(text: String, tips: List<String>, bypassChecks: Boolean = false, onClick: () -> Unit): Renderable {
            return clickable(hoverTips(text, tips, bypassChecks = bypassChecks), onClick, bypassChecks = bypassChecks)
        }

        fun clickable(render: Renderable, onClick: () -> Unit, button: Int = 0, bypassChecks: Boolean = false, condition: () -> Boolean = { true }) =
            object : Renderable {
                override val width: Int
                    get() = render.width
                override val height = 10

                private var wasDown = false

                override fun render(posX: Int, posY: Int) {
                    val isDown = Mouse.isButtonDown(button)
                    if (isDown > wasDown && isHovered(posX, posY)) {
                        if (condition() && shouldAllowLink(true, bypassChecks)) {
                            onClick()
                        }
                    }
                    wasDown = isDown
                    render.render(posX, posY)
                }
            }

        fun hoverTips(text: String, tips: List<String>, indexes: List<Int> = listOf(), stack: ItemStack? = null, bypassChecks: Boolean = false, condition: () -> Boolean = { true }): Renderable {
            val render = string(text)
            return object : Renderable {
                override val width: Int
                    get() = render.width
                override val height = 11

                override fun render(posX: Int, posY: Int) {
                    render.render(posX, posY)
                    if (isHovered(posX, posY)) {
                        if (condition() && shouldAllowLink(true, bypassChecks)) {
                            list[Pair(posX, posY)] = indexes
                            drawHoveringText(posX, posY, tips, stack)
                        }
                    } else {
                        if (list.contains(Pair(posX, posY))) {
                            list.remove(Pair(posX, posY))
                        }
                    }
                }
            }
        }

        private fun drawHoveringText(posX: Int, posY: Int, tips: List<String?>, stack: ItemStack? = null) {
            if (tips.isNotEmpty()) {
                var textLines = tips
                val x = Utils.getMouseX() + 12 - posX
                val y = Utils.getMouseY() - 10 - posY
                val color: Char = stack?.getItemCharRarity() ?: Utils.getPrimaryColourCode(textLines[0])
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
                    for (i in textLines.indices) {
                        val textLine = textLines[i]
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
                for (lineNumber in textLines.indices) {
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

        private fun drawGradientRect(zLevel: Int, left: Int, top: Int, right: Int, bottom: Int, startColor: Int, endColor: Int) {
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
            worldrenderer.pos(right.toDouble(), top.toDouble(), zLevel.toDouble()).color(startRed, startGreen, startBlue, startAlpha).endVertex()
            worldrenderer.pos(left.toDouble(), top.toDouble(), zLevel.toDouble()).color(startRed, startGreen, startBlue, startAlpha).endVertex()
            worldrenderer.pos(left.toDouble(), bottom.toDouble(), zLevel.toDouble()).color(endRed, endGreen, endBlue, endAlpha).endVertex()
            worldrenderer.pos(right.toDouble(), bottom.toDouble(), zLevel.toDouble()).color(endRed, endGreen, endBlue, endAlpha).endVertex()
            tessellator.draw()
            GlStateManager.shadeModel(7424)
            GlStateManager.disableBlend()
            GlStateManager.enableAlpha()
            GlStateManager.enableTexture2D()
        }

        private fun shouldAllowLink(debug: Boolean = false, bypassChecks: Boolean): Boolean {
            val isGuiScreen = Minecraft.getMinecraft().currentScreen != null
            if (bypassChecks) {
                return isGuiScreen
            }
            val isGuiPositionEditor = Minecraft.getMinecraft().currentScreen !is GuiPositionEditor
            val isNotInSignAndOnSlot = if (Minecraft.getMinecraft().currentScreen !is GuiEditSign) {
                ToolTipData.lastSlot == null
            } else true
            val isConfigScreen = Minecraft.getMinecraft().currentScreen !is GuiScreenElementWrapper
            val result = isGuiScreen && isGuiPositionEditor && isNotInSignAndOnSlot && isConfigScreen

            if (debug) {
                if (!result) {
                    logger.log("")
                    logger.log("blocked link because:")
                    if (!isGuiScreen) logger.log("isGuiScreen")
                    if (!isGuiPositionEditor) logger.log("isGuiPositionEditor")
                    if (!isNotInSignAndOnSlot) logger.log("isNotInSignAndOnSlot")
                    if (!isConfigScreen) logger.log("isConfigScreen")
                    logger.log("")
                } else {
                    logger.log("allowed click")
                }
            }

            return result
        }

        fun underlined(renderable: Renderable) = object : Renderable {
            override val width: Int
                get() = renderable.width
            override val height = 10

            override fun render(posX: Int, posY: Int) {
                Gui.drawRect(0, 10, width, 11, 0xFFFFFFFF.toInt())
                GlStateManager.color(1F, 1F, 1F, 1F)
                renderable.render(posX, posY)
            }
        }

        fun hoverable(hovered: Renderable, unhovered: Renderable, bypassChecks: Boolean = false, condition: () -> Boolean = { true }) =
            object : Renderable {
                override val width: Int
                    get() = max(hovered.width, unhovered.width)
                override val height = 10

                override fun render(posX: Int, posY: Int) {
                    if (isHovered(posX, posY) && condition() && shouldAllowLink(true, bypassChecks))
                        hovered.render(posX, posY)
                    else
                        unhovered.render(posX, posY)
                }
            }

        fun itemStack(any: ItemStack, scale: Double = 1.0) = object : Renderable {
            override val width: Int
                get() = 12
            override val height = 10

            override fun render(posX: Int, posY: Int) {
                any.renderOnScreen(0F, 0F, scaleMultiplier = scale)
            }
        }

        fun string(string: String) = object : Renderable {
            override val width: Int
                get() = Minecraft.getMinecraft().fontRendererObj.getStringWidth(string)
            override val height = 10

            override fun render(posX: Int, posY: Int) {
                Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow("§f$string", 1f, 1f, 0)
            }
        }

        fun placeholder(width: Int) = object : Renderable {
            override val width: Int = width
            override val height = 10

            override fun render(posX: Int, posY: Int) {
            }
        }
    }
}