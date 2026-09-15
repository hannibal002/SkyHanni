package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.NumberUtil.fractionOf
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.inventory.ChestMenu

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

    private val inventory = InventoryDetector { inventoryPattern }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.enabled) return
        if (!inventory.isInside()) return

        // Coerce the thresholds into order at read time so ranges never overlap, no matter how the
        // sliders are set (veryBusy >= busy >= moderate).
        val moderate = config.moderateThreshold
        val busy = config.busyThreshold.coerceAtLeast(moderate)
        val veryBusy = config.veryBusyThreshold.coerceAtLeast(busy)

        val chest = event.container as ChestMenu
        for ((slot, stack) in chest.getUpperItems()) {
            playersPattern.firstMatcher(stack.getCleanLore()) {
                val current = group("current").toInt()
                val max = group("max").toInt()
                val percent = current.fractionOf(max) * 100

                val color = when {
                    percent >= veryBusy -> config.veryBusyColor
                    percent >= busy -> config.busyColor
                    percent >= moderate -> config.moderateColor
                    else -> config.quietColor
                }
                slot.highlight(color)
            }
        }
    }
}
