package at.hannibal2.skyhanni.features.misc.massconfiguration

import io.github.moulberry.moulconfig.internal.GlScissorStack
import io.github.moulberry.moulconfig.internal.RenderUtils
import io.github.moulberry.moulconfig.internal.TextRenderUtils
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse

class DefaultConfigOptionGui(
    val orderedOptions: Map<FeatureToggleProcessor.Category, List<FeatureToggleProcessor.FeatureToggleableOption>>,
    old: String,
    new: String
) :
    GuiScreen() {
    val title = if (old == "null") {
        if (new == "null")
            "§5SkyHanni Default Options"
        else
            "§5SkyHanni Options In Version $new"
    } else {
        if (new == "null") {
            "§5SkyHanni Options since $old"
        } else {
            "§5SkyHanni Options $old → $new"
        }
    }
    val xSize = 400
    val ySize = 300
    val barSize = 40
    val padding = 10
    var wasMouseDown = false
    val cardHeight = 30

    var currentScrollOffset = 0

    enum class ResetSuggestionState(val label: String) {
        TURN_ALL_OFF("§c§lTurn all off"),
        TURN_ALL_ON("§a§lTurn all on"),
        LEAVE_DEFAULTS("§b§lLeave unchanged"), ;

        val next get() = entries[(ordinal + 1) % entries.size]
    }

    val resetSuggestionState =
        orderedOptions.keys.associateWith { ResetSuggestionState.LEAVE_DEFAULTS }.toMutableMap()

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        drawDefaultBackground()
        RenderUtils.drawFloatingRectDark((width - xSize) / 2, (height - ySize) / 2, xSize, ySize)
        val sr = ScaledResolution(mc)
        var hoveringTextToDraw: List<String>? = null
        var mx = mouseX - ((width - xSize) / 2) - padding
        val isMouseDown = Mouse.isButtonDown(0)
        val shouldClick = isMouseDown && !wasMouseDown
        wasMouseDown = isMouseDown
        val isMouseInScrollArea =
            mx in 0..xSize && mouseY in ((height - ySize) / 2) + barSize..((height + ySize) / 2 - barSize)
        var my = mouseY - ((height - ySize) / 2 + barSize) + currentScrollOffset

        GlStateManager.pushMatrix()
        GlStateManager.translate(width / 2F, (height - ySize) / 2F, 0F)
        GlStateManager.scale(2f, 2f, 1f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(
            title,
            mc.fontRendererObj,
            0F,
            mc.fontRendererObj.FONT_HEIGHT.toFloat(),
            false,
            xSize / 2 - padding,
            -1
        )
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(
            (width - xSize) / 2F + padding,
            (height + ySize) / 2F - mc.fontRendererObj.FONT_HEIGHT * 2,
            0F
        )
        var i = 0
        fun bt(title: String, tooltip: List<String>, func: () -> Unit) {
            val lw = mc.fontRendererObj.getStringWidth(title)
            var s = false
            if (mouseX - ((width - xSize) / 2 + padding) in i..(i + lw)
                && mouseY - (height + ySize) / 2 in -barSize..0
            ) {
                s = true
                hoveringTextToDraw = tooltip
                if (shouldClick) {
                    func()
                }
            }
            RenderUtils.drawFloatingRectDark(i - 1, -3, lw + 4, 14)
            mc.fontRendererObj.drawString(title, 2 + i.toFloat(), 0F, if (s) 0xFF00FF00.toInt() else -1, s)
            i += lw + 12
        }
        bt("Apply choices", listOf()) {
            DefaultConfigFeatures.applyCategorySelections(resetSuggestionState, orderedOptions)
            mc.displayGuiScreen(null)
        }
        bt("Turn all on", listOf()) {
            resetSuggestionState.entries.forEach {
                it.setValue(ResetSuggestionState.TURN_ALL_ON);
                orderedOptions[it.key]!!.forEach { it.toggleOverride = null }
            }
        }
        bt("Turn all off", listOf()) {
            resetSuggestionState.entries.forEach {
                it.setValue(ResetSuggestionState.TURN_ALL_OFF)
                orderedOptions[it.key]!!.forEach { it.toggleOverride = null }
            }
        }
        bt("Leave all untouched", listOf()) {
            resetSuggestionState.entries.forEach {
                it.setValue(ResetSuggestionState.LEAVE_DEFAULTS)
                orderedOptions[it.key]!!.forEach { it.toggleOverride = null }
            }
        }
        bt("Cancel", listOf()) {
            mc.displayGuiScreen(null)
        }
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlScissorStack.push(
            (width - xSize) / 2,
            (height - ySize) / 2 + barSize,
            (width + xSize) / 2,
            (height + ySize) / 2 - barSize,
            sr
        )
        GlStateManager.translate(
            (width - xSize) / 2F + padding,
            (height - ySize) / 2F + barSize - currentScrollOffset,
            0F
        )

        for ((cat) in orderedOptions.entries) {
            val suggestionState = resetSuggestionState[cat]!!
            drawRect(0, 0, xSize - padding * 2, 1, 0xFF808080.toInt())
            drawRect(0, 30, xSize - padding * 2, cardHeight + 1, 0xFF808080.toInt())
            drawRect(0, 0, 1, cardHeight, 0xFF808080.toInt())
            drawRect(xSize - padding * 2 - 1, 0, xSize - padding * 2, cardHeight, 0xFF808080.toInt())
            mc.fontRendererObj.drawString("§e${cat.name} ${suggestionState.label}", 4, 4, -1)
            mc.fontRendererObj.drawSplitString("§7${cat.description}", 4, 14, xSize - padding * 2 - 8, -1)
            if (isMouseInScrollArea && my in 0..cardHeight) {
                hoveringTextToDraw =
                    listOf(
                        "§e${cat.name}",
                        "§7${cat.description}",
                        "§7Current plan: ${suggestionState.label}",
                        "§aClick to toggle!",
                        "§7Hold shift to show all options"
                    )
                if (isShiftKeyDown()) {
                    hoveringTextToDraw = listOf(
                        "§e${cat.name}",
                        "§7${cat.description}",
                    ) + orderedOptions[cat]!!.map { "§7 - §a" + it.name }
                }
                if (shouldClick) {
                    resetSuggestionState[cat] = suggestionState.next
                    orderedOptions[cat]!!.forEach { it.toggleOverride = null }
                }
            }
            my -= cardHeight
            GlStateManager.translate(0F, cardHeight.toFloat(), 0F)
        }


        GlStateManager.popMatrix()
        GlScissorStack.pop(sr)
        if (hoveringTextToDraw != null) {
            Utils.drawHoveringText(hoveringTextToDraw, mouseX, mouseY, width, height, 100, mc.fontRendererObj)
        }

    }

    fun scroll(s: Int) {
        currentScrollOffset =
            Math.max(0, Math.min(s, (orderedOptions.size + 1) * cardHeight - ySize + barSize + padding * 2))
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        if (Mouse.getEventDWheel() != 0)
            scroll(currentScrollOffset - Mouse.getEventDWheel())
    }
}