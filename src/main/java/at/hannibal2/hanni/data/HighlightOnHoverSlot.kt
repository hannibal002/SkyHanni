package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.InventoryOpenEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.RenderUtils.highlight

@HanniModule
object HighlightOnHoverSlot {
    val currentSlots = mutableMapOf<Pair<Int, Int>, List<Int>>()

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        currentSlots.clear()
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        currentSlots.clear()
    }

    @HandleEvent(priority = HandleEvent.LOW, onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        val list = currentSlots.flatMapTo(mutableSetOf()) { it.value }
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotNumber in list) {
                slot.highlight(LorenzColor.GREEN)
            }
        }
    }
}
