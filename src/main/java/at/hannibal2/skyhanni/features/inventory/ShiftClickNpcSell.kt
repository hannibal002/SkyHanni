package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils.makeShiftClick
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object ShiftClickNpcSell {

    private val config get() = HanniMod.feature.inventory.shiftClickNpcSell

    private const val SELL_SLOT = -4

    /**
     * REGEX-TEST: §eClick to buyback!
     */
    private val lastLoreLineOfSellPattern by RepoPattern.pattern(
        "inventory.npc.sell.lore",
        "§7them to this Shop!|§eClick to buyback!",
    )

    var inInventory = false
        private set

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryItems.isEmpty()) return
        val item = event.inventoryItems[event.inventoryItems.keys.last() + SELL_SLOT] ?: return

        inInventory = lastLoreLineOfSellPattern.matches(item.getLore().lastOrNull())
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        val slot = event.slot ?: return

        if (slot.slotNumber == slot.slotIndex) return

        event.makeShiftClick()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(97, "inventory.shiftClickNPCSell", "inventory.shiftClickNpcSell")
    }
}
