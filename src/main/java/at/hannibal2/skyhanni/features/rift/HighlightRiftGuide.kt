package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightRiftGuide {
    private val config get() = SkyHanniMod.feature.rift
    private var inInventory = false
    private var highlightedItems = listOf<Int>()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inInventory = false

        if (!isEnabled()) return

        val inGuide = event.inventoryItems[40]?.getLore()?.let {
            if (it.size == 1) {
                it[0] == "§7To Rift Guide"
            } else false
        } ?: false
        if (!inGuide) return

        val highlightedItems = mutableListOf<Int>()
        for ((slot, stack) in event.inventoryItems) {
            val lore = stack.getLore()
            if (lore.isNotEmpty()) {
                if (lore.last() == "§8✖ Not completed yet!") {
                    highlightedItems.add(slot)
                }
            }
        }
        inInventory = true
        this.highlightedItems = highlightedItems
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex in highlightedItems) {
                slot highlight LorenzColor.YELLOW
            }
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.highlightGuide
}