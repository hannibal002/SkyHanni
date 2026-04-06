package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.compat.SkyHanniScreenTheme
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.labeledCard
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft

object DefaultConfigGui {

    private const val CARD_WIDTH = 380
    private const val LIST_HEIGHT = 250

    fun buildContent(screen: DefaultConfigScreen): Renderable {
        val categoryCards = screen.orderedOptions.entries.map { (cat, _) ->
            buildCategoryCard(screen, cat)
        }

        val list = Renderable.scrollList(
            categoryCards,
            height = LIST_HEIGHT,
            scrollValue = screen.scrollValue,
            showScrollbar = true,
            scrollbarTrackColor = SkyHanniScreenTheme.COLOR_SCROLLBAR_TRACK,
            scrollbarThumbColor = SkyHanniScreenTheme.COLOR_SCROLLBAR_THUMB,
            button = 0,
        )

        val actionBar = buildActionBar(screen)
        return Renderable.vertical(listOf(list, actionBar), spacing = 8)
    }

    private fun buildCategoryCard(screen: DefaultConfigScreen, cat: Category): Renderable {
        val state = screen.resetSuggestionState[cat] ?: ResetSuggestionState.LEAVE_DEFAULTS
        val card = Renderable.labeledCard(
            "§e${cat.name} ${state.label}",
            Renderable.text("§7${cat.description}"),
            CARD_WIDTH,
        )

        val normalTooltip = buildList {
            add(Renderable.text("§e${cat.name}"))
            add(Renderable.text("§7${cat.description}"))
            add(Renderable.text("§7Current plan: ${state.label}"))
            add(Renderable.text("§aClick to toggle!"))
            add(Renderable.text("§7Hold shift to show all options"))
        }
        val shiftTooltip = buildList {
            add(Renderable.text("§e${cat.name}"))
            add(Renderable.text("§7${cat.description}"))
            addAll(screen.orderedOptions[cat]?.map { Renderable.text("§7 - §a${it.name}") }.orEmpty())
        }

        val withHover = Renderable.hoverable(
            Renderable.drawInsideRoundedRect(card, SkyHanniScreenTheme.COLOR_ROW_HOVER, padding = 3, radius = 5),
            Renderable.drawInsideRoundedRect(card, SkyHanniScreenTheme.COLOR_ROW_NORMAL, padding = 3, radius = 5),
            bypassChecks = true,
        )

        val withTooltip = object : Renderable by withHover {
            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                withHover.render(mouseOffsetX, mouseOffsetY)
                if (isHovered(mouseOffsetX, mouseOffsetY)) {
                    val tip = if (KeyboardManager.isShiftKeyDown()) shiftTooltip else normalTooltip
                    RenderableTooltips.setTooltipForRender(tip)
                }
            }
        }

        return Renderable.clickable(
            withTooltip,
            onLeftClick = {
                screen.resetSuggestionState[cat] = state.next
                screen.orderedOptions[cat]?.forEach { it.toggleOverride = null }
                screen.rebuildDisplay()
            },
            bypassChecks = true,
        )
    }

    private fun buildActionBar(screen: DefaultConfigScreen): Renderable {
        val mc = Minecraft.getInstance()
        val applyBtn = SkyHanniScreenTheme.buildButton("Apply choices", SkyHanniScreenTheme.COLOR_BTN_PRIMARY) {
            DefaultConfigFeatures.applyCategorySelections(screen.resetSuggestionState, screen.orderedOptions)
            mc.setScreen(null)
        }
        val allOnBtn = SkyHanniScreenTheme.buildButton("Turn all on", SkyHanniScreenTheme.COLOR_BTN_NEUTRAL) {
            for (entry in screen.resetSuggestionState.entries) {
                entry.setValue(ResetSuggestionState.TURN_ALL_ON)
                screen.orderedOptions[entry.key]?.forEach { it.toggleOverride = null }
            }
            screen.rebuildDisplay()
        }
        val allOffBtn = SkyHanniScreenTheme.buildButton("Turn all off", SkyHanniScreenTheme.COLOR_BTN_NEUTRAL) {
            for (entry in screen.resetSuggestionState.entries) {
                entry.setValue(ResetSuggestionState.TURN_ALL_OFF)
                screen.orderedOptions[entry.key]?.forEach { it.toggleOverride = null }
            }
            screen.rebuildDisplay()
        }
        val leaveBtn = SkyHanniScreenTheme.buildButton("Leave all untouched", SkyHanniScreenTheme.COLOR_BTN_NEUTRAL) {
            for (entry in screen.resetSuggestionState.entries) {
                entry.setValue(ResetSuggestionState.LEAVE_DEFAULTS)
                screen.orderedOptions[entry.key]?.forEach { it.toggleOverride = null }
            }
            screen.rebuildDisplay()
        }
        val cancelBtn = SkyHanniScreenTheme.buildButton("Cancel", SkyHanniScreenTheme.COLOR_BTN_DANGER) {
            mc.setScreen(null)
        }

        return Renderable.horizontal(listOf(applyBtn, allOnBtn, allOffBtn, leaveBtn, cancelBtn), spacing = 10)
    }
}
