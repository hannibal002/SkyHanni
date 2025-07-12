package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.StringUtils.splitLines
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import kotlin.math.max
import kotlin.math.min

class DefaultConfigOptionGui(
    private val orderedOptions: Map<Category, List<FeatureToggleableOption>>,
    old: String,
    new: String,
) : SkyhanniBaseScreen() {

    private val guiTitle = if (old == "null") {
        if (new == "null")
            "§5SkyHanni Default Options"
        else
            "§5SkyHanni Options In Version $new"
    } else {
        if (new == "null")
            "§5SkyHanni Options since $old"
        else
            "§5SkyHanni Options $old → $new"
    }

    private val xSize = 400
    private val ySize = 300
    private val barSize = 40
    private val padding = 10
    private var wasMouseDown = false
    private val cardHeight = 30

    private var currentScrollOffset = 0

    private val resetSuggestionState =
        orderedOptions.keys.associateWith { ResetSuggestionState.LEAVE_DEFAULTS }.toMutableMap()

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun onDrawScreen(originalMouseX: Int, originalMouseY: Int, partialTicks: Float) {
        drawDefaultBackground(originalMouseX, originalMouseY, partialTicks)
        GuiRenderUtils.drawFloatingRectDark((width - xSize) / 2, (height - ySize) / 2, xSize, ySize)
        var hoveringTextToDraw: List<String>? = null
        val x = originalMouseX - ((width - xSize) / 2) - padding
        val isMouseDown = MouseCompat.isButtonDown(0)
        val shouldClick = isMouseDown && !wasMouseDown
        wasMouseDown = isMouseDown
        val isMouseInScrollArea =
            x in 0..xSize && originalMouseY in ((height - ySize) / 2) + barSize..((height + ySize) / 2 - barSize)
        var y = originalMouseY - ((height - ySize) / 2 + barSize) + currentScrollOffset

        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate(width / 2F, (height - ySize) / 2F, 0F)
        DrawContextUtils.scale(2f, 2f, 1f)
        GuiRenderUtils.drawStringCenteredScaledMaxWidth(
            guiTitle,
            0F,
            mc.fontRendererObj.FONT_HEIGHT.toFloat(),
            false,
            xSize / 2 - padding,
            -1,
        )
        DrawContextUtils.popMatrix()

        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate(
            (width - xSize) / 2F + padding,
            (height + ySize) / 2F - mc.fontRendererObj.FONT_HEIGHT * 2,
            0F,
        )
        var i = 0
        fun button(title: String, tooltip: List<String>, func: () -> Unit) {
            val width = mc.fontRendererObj.getStringWidth(title)
            var overMouse = false
            if (originalMouseX - ((this.width - xSize) / 2 + padding) in i..(i + width) &&
                originalMouseY - (height + ySize) / 2 in -barSize..0
            ) {
                overMouse = true
                hoveringTextToDraw = tooltip
                if (shouldClick) {
                    func()
                }
            }
            GuiRenderUtils.drawFloatingRectDark(i - 1, -3, width + 4, 14)
            GuiRenderUtils.drawString(
                title,
                2 + i.toFloat(),
                0F,
                if (overMouse) 0xFF00FF00.toInt() else -1,
                overMouse,
            )
            i += width + 12
        }
        button("Apply choices", listOf()) {
            DefaultConfigFeatures.applyCategorySelections(resetSuggestionState, orderedOptions)
            mc.displayGuiScreen(null)
        }
        button("Turn all on", listOf()) {
            for (entry in resetSuggestionState.entries) {
                entry.setValue(ResetSuggestionState.TURN_ALL_ON)
                orderedOptions[entry.key]?.let { opts ->
                    opts.forEach { it.toggleOverride = null }
                }
            }
        }
        button("Turn all off", listOf()) {
            for (entry in resetSuggestionState.entries) {
                entry.setValue(ResetSuggestionState.TURN_ALL_OFF)
                orderedOptions[entry.key]?.let { opts ->
                    opts.forEach { it.toggleOverride = null }
                }
            }
        }
        button("Leave all untouched", listOf()) {
            for (entry in resetSuggestionState.entries) {
                entry.setValue(ResetSuggestionState.LEAVE_DEFAULTS)
                orderedOptions[entry.key]?.let { opts ->
                    opts.forEach { it.toggleOverride = null }
                }
            }
        }
        button("Cancel", listOf()) {
            mc.displayGuiScreen(null)
        }
        DrawContextUtils.popMatrix()

        DrawContextUtils.pushMatrix()
        GuiRenderUtils.enableScissor(
            (width - xSize) / 2,
            (height - ySize) / 2 + barSize,
            (width + xSize) / 2,
            (height + ySize) / 2 - barSize,
        )
        DrawContextUtils.translate(
            (width - xSize) / 2F + padding,
            (height - ySize) / 2F + barSize - currentScrollOffset,
            0F,
        )

        for ((cat) in orderedOptions.entries) {
            val suggestionState = resetSuggestionState[cat]!!

            GuiRenderUtils.drawRect(0, 0, xSize - padding * 2, 1, 0xFF808080.toInt())
            GuiRenderUtils.drawRect(0, 30, xSize - padding * 2, cardHeight + 1, 0xFF808080.toInt())
            GuiRenderUtils.drawRect(0, 0, 1, cardHeight, 0xFF808080.toInt())
            GuiRenderUtils.drawRect(xSize - padding * 2 - 1, 0, xSize - padding * 2, cardHeight, 0xFF808080.toInt())

            GuiRenderUtils.drawString("§e${cat.name} ${suggestionState.label}", 4, 4)
            GuiRenderUtils.drawStrings("§7${cat.description}".splitLines(xSize - padding * 2 - 8), 4, 14, -1)

            if (isMouseInScrollArea && y in 0..cardHeight) {
                hoveringTextToDraw = listOf(
                    "§e${cat.name}",
                    "§7${cat.description}",
                    "§7Current plan: ${suggestionState.label}",
                    "§aClick to toggle!",
                    "§7Hold shift to show all options",
                )

                if (KeyboardManager.isShiftKeyDown()) {
                    hoveringTextToDraw = listOf(
                        "§e${cat.name}",
                        "§7${cat.description}",
                    ) + orderedOptions[cat]?.let { opts ->
                        opts.map { "§7 - §a" + it.name }
                    }.orEmpty()
                }

                if (shouldClick) {
                    resetSuggestionState[cat] = suggestionState.next
                    orderedOptions[cat]?.let { opts ->
                        opts.forEach { it.toggleOverride = null }
                    }
                }
            }

            y -= cardHeight
            DrawContextUtils.translate(0F, cardHeight.toFloat(), 0F)
        }

        DrawContextUtils.popMatrix()
        GuiRenderUtils.disableScissor()
        hoveringTextToDraw?.let { tooltip ->
            RenderableTooltips.setTooltipForRender(tooltip.map(StringRenderable::from))
        }
    }

    private fun scroll(s: Int) {
        currentScrollOffset =
            max(0, min(s, (orderedOptions.size + 1) * cardHeight - ySize + barSize + padding * 2))
    }

    override fun onHandleMouseInput() {
        if (MouseCompat.getScrollDelta() != 0)
            scroll(currentScrollOffset - MouseCompat.getScrollDelta())
    }
}
