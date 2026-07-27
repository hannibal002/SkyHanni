package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object HubSelectorCapacityHighlight {

    private val config get() = SkyHanniMod.feature.inventory.hubSelector

    private val patternGroup = RepoPattern.group("inventory.hubselector")

    /**
     * REGEX-TEST: SkyBlock Hub Selector
     * REGEX-TEST: Skyblock Hub Selector
     */
    private val inventoryPattern by patternGroup.pattern(
        "title",
        "(?i)SkyBlock Hub Selector",
    )

    /**
     * REGEX-TEST: Players: 54/60
     * REGEX-TEST: Players: 60/60
     */
    private val playersPattern by patternGroup.pattern(
        "lobby.players",
        "Players: (?<current>\\d+)/(?<max>\\d+)",
    )

    private val hubSelectorInventory = InventoryDetector { inventoryPattern }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.enabled) return
        if (!hubSelectorInventory.isInside()) return

        for (slot in event.container.slots) {
            playersPattern.firstMatcher(slot.item.getCleanLore()) {
                val current = group("current").toInt()
                val color = when {
                    current >= 45 -> config.veryBusyColor
                    current >= 30 -> config.busyColor
                    current >= 15 -> config.moderateColor
                    else -> config.quietColor
                }
                slot.highlight(color)
            }
        }
    }
}
