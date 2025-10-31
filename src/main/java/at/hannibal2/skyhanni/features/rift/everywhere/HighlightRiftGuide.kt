package at.hannibal2.hanni.features.rift.everywhere

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.RenderUtils.highlight

@HanniModule
object HighlightRiftGuide {

    private var inInventory = false
    private var highlightedItems = emptyList<Int>()

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inInventory = false

        if (!isEnabled()) return

        val inGuide = event.inventoryItems[40]?.getLore()?.let {
            if (it.size == 1) {
                it[0].startsWith("§7To Rift Guide")
            } else false
        } ?: false
        if (!inGuide) return

        val highlightedItems = mutableListOf<Int>()
        for ((slot, stack) in event.inventoryItems) {
            val lore = stack.getLore()
            if (lore.isNotEmpty() && lore.last() == "§8✖ Not completed yet!") {
                highlightedItems.add(slot)
            }
        }
        inInventory = true
        this.highlightedItems = highlightedItems
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex in highlightedItems) {
                slot.highlight(LorenzColor.YELLOW)
            }
        }
    }

    fun isEnabled() = RiftApi.inRift() && RiftApi.config.highlightGuide
}
