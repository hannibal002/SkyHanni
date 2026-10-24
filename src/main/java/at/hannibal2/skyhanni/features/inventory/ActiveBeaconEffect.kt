package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ActiveBeaconEffect {

    val config get() = SkyHanniMod.feature.inventory

    private val patternGroup = RepoPattern.group("inventory.activebeaconeffect")

    /**
     * REGEX-TEST: Profile Stat Upgrades
     */
    private val inventoryPattern by patternGroup.pattern(
        "inventory",
        "Profile Stat Upgrades",
    )

    /**
     * REGEX-TEST: Active stat boost!
     */
    private val slotPattern by patternGroup.pattern(
        "slot.active.colorless",
        "Active stat boost!",
    )

    private var slot: Int? = null

    @HandleEvent(onlyOnIsland = PRIVATE_ISLAND)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!inventoryPattern.matches(event.inventoryName)) {
            slot = null
            return
        }

        slot = event.inventoryItems.filter { (_, stack) ->
            slotPattern.anyMatches(stack.getCleanLore())
        }.firstNotNullOfOrNull { it.key }
    }

    @HandleEvent(onlyOnIsland = PRIVATE_ISLAND)
    private fun onInventoryClose() {
        slot = null
    }

    @HandleEvent(onlyOnIsland = PRIVATE_ISLAND)
    private fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        val slot = slot ?: return

        event.container.getSlot(slot).highlight(LorenzColor.GREEN)
    }

    fun isEnabled() = config.highlightActiveBeaconEffect
}
