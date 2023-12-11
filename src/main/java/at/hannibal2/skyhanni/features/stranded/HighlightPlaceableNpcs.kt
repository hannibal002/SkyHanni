package at.hannibal2.skyhanni.features.stranded

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightPlaceableNpcs {
    private val config get() = SkyHanniMod.feature.stranded.highlightPlaceableNpcs
    private val locationPattern = "§7Location: §f\\[§e\\d+§f, §e\\d+§f, §e\\d+§f]".toPattern()

    private var inInventory = false
    private var highlightedItems = emptyList<Int>()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inInventory = false
        if (!isEnabled()) return

        if (event.inventoryName != "Island NPCs") return

        val highlightedItems = mutableListOf<Int>()
        for ((slot, stack) in event.inventoryItems) {
            if (isPlaceableNpc(stack.getLore())) {
                highlightedItems.add(slot)
            }
        }
        inInventory = true
        this.highlightedItems = highlightedItems
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        highlightedItems = emptyList()
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled() || !inInventory) return
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex in highlightedItems) {
                slot highlight LorenzColor.GREEN
            }
        }
    }

    private fun isPlaceableNpc(lore: List<String>): Boolean {
        // Checking if NPC & placeable
        if (lore.isEmpty() || !(lore.last() == "§ethis NPC!" || lore.last() == "§eyour location!")) {
            return false
        }

        // Checking if is already placed
        for (line in lore) {
            if (locationPattern.matcher(line).matches()) return false
        }
        return true
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config
}
