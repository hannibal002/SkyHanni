package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.SackOpenEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.item.Items

@SkyHanniModule
object MoveInsertIntoSackButton {
    private val config get() = SkyHanniMod.feature.misc.moveInsertIntoSackButton

    private var inSackMenu = false
    private var insertIntoSackItem: SafeItemStack? = null
    private var originalSlotItem: SafeItemStack? = null

    private var chestSlot = -1
    private var swapTargetSlot = -1

    @HandleEvent
    fun onSackOpen(event: SackOpenEvent) {
        if (!config) return
        val openEvent = event.inventoryOpenEvent ?: return
        val items = openEvent.inventoryItems
        val size = openEvent.inventorySize

        // Dynamically compute positions based on rows of 9 columns
        val computedChestSlot = size - 3
        val computedTargetSlot = computedChestSlot - 3

        if (computedTargetSlot < 0) return

        val item = items[computedChestSlot]
        if (item == null || item.getItem() != Items.CHEST) return

        inSackMenu = true
        chestSlot = computedChestSlot
        swapTargetSlot = computedTargetSlot
        insertIntoSackItem = item
        originalSlotItem = items[swapTargetSlot]
    }

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return

        val newSlotItem = when (event.slot) {
            swapTargetSlot -> insertIntoSackItem
            chestSlot -> originalSlotItem
            else -> return
        } ?: return

        event.replace(newSlotItem)
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return

        val newSlotId = when (event.slotId) {
            swapTargetSlot -> chestSlot
            chestSlot -> swapTargetSlot
            else -> return
        }
        event.redirectClick(newSlotId)
    }

    @HandleEvent
    fun onInventoryClose() {
        inSackMenu = false
        insertIntoSackItem = null
        originalSlotItem = null
        chestSlot = -1
        swapTargetSlot = -1
    }

    fun isEnabled() = config && inSackMenu
}
