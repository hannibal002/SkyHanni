package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.SackOpenEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.item.Items

@SkyHanniModule
object MoveInsertIntoSackButton {
    private var inSackMenu = false
    private var insertIntoSackItem: SafeItemStack? = null
    private var originalSlot48Item: SafeItemStack? = null

    private const val CHEST_SLOT = 51
    private const val SWAP_TARGET_SLOT = 48

    @HandleEvent
    fun onSackOpen(event: SackOpenEvent) {
        val openEvent = event.inventoryOpenEvent
        val items = openEvent.inventoryItems

        val item = items[CHEST_SLOT]
        if (item == null || item.getItem() != Items.CHEST) return

        inSackMenu = true
        insertIntoSackItem = item
        originalSlot48Item = items[SWAP_TARGET_SLOT]
    }

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return

        val newSlotItem = when (event.slot) {
            SWAP_TARGET_SLOT -> insertIntoSackItem
            CHEST_SLOT -> originalSlot48Item
            else -> return
        } ?: return

        event.replace(newSlotItem)

    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return

        val newSlotId = when (event.slotId) {
            SWAP_TARGET_SLOT -> CHEST_SLOT
            CHEST_SLOT -> SWAP_TARGET_SLOT
            else -> return
        }
        event.redirectClick(newSlotId)
    }

    @HandleEvent
    fun onInventoryClose() {
        inSackMenu = false
        insertIntoSackItem = null
        originalSlot48Item = null
    }

    fun isEnabled() = inSackMenu
}
