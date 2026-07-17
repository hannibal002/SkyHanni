package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object TabWidgetSettings {
    private val patternGroup = RepoPattern.group("tab.widget.setting")

    /**
     * REGEX-TEST: Widgets on Private Island
     * REGEX-TEST: Widgets in Crystal Hollows
     * REGEX-TEST: (1/2) Widgets on Galatea
     * REGEX-TEST: (1/2) Widgets in Crystal Hollows
     */
    private val mainPageSettingPattern by patternGroup.pattern(
        "gui",
        "^(?:\\(\\d+/\\d+\\) )?Widgets (?:in|on) .*$",
    )

    /**
     * REGEX-TEST: Currently: ALWAYS ENABLED
     * REGEX-TEST: Currently: DISABLED
     */
    private val mainPageWidgetPattern by patternGroup.pattern(
        "main.colorless",
        "Currently:.*",
    )

    /**
     * REGEX-TEST: Click to disable!
     * REGEX-TEST: Click to edit!
     */
    private val subPageWidgetPattern by patternGroup.pattern(
        "sub.colorless",
        "Click to .*",
    )

    /**
     * REGEX-TEST: Profile Widget Settings
     */
    private val shownSettingPattern by patternGroup.pattern(
        "show",
        "Shown .* Setting.*|.*Widget Settings",
    )

    /**
     * REGEX-TEST: Click to disable!
     */
    private val clickToDisablePattern by patternGroup.pattern(
        "click.disable.colorless",
        ".*disable!",
    )

    /**
     * REGEX-TEST: Currently: ENABLED
     */
    private val enabledPattern by patternGroup.pattern(
        "is.enabled.colorless",
        ".*ENABLED",
    )

    var inInventory = false
    var highlights = mutableMapOf<Int, LorenzColor>()

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        highlights.clear()

        val inventoryName = event.inventoryName
        if (mainPageSettingPattern.matches(inventoryName)) {
            inInventory = true
            val items = event.inventoryItems.filter { mainPageWidgetPattern.anyMatches(it.value.getCleanLore()) }
            for ((slot, stack) in items) {
                highlights[slot] = if (enabledPattern.anyMatches(stack.getCleanLore())) {
                    LorenzColor.GREEN
                } else {
                    LorenzColor.RED
                }
            }
        }

        if (shownSettingPattern.matches(inventoryName)) {
            inInventory = true
            val items = event.inventoryItems.filter {
                subPageWidgetPattern.matches(it.value.getCleanLore().lastOrNull())
            }

            for ((slot, stack) in items) {
                highlights[slot] = if (clickToDisablePattern.anyMatches(stack.getCleanLore())) {
                    LorenzColor.GREEN
                } else {
                    LorenzColor.RED
                }
            }
        }
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        highlights.clear()
    }

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        event.container.slots
            .associateWith { highlights[it.index] }
            .forEach { (slot, color) ->
                color?.let { slot.highlight(it) }
            }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && SkyHanniMod.feature.inventory.highlightWidgets
}
