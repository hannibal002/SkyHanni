package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.InventoryUtils.makeShiftClick
import net.minecraft.client.gui.inventory.GuiChest

@HanniModule
object ShiftClickEquipment {

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!HanniMod.feature.inventory.shiftClickForEquipment) return

        if (event.gui !is GuiChest) return

        val slot = event.slot ?: return

        if (slot.slotNumber == slot.slotIndex) return

        if (slot.stack == null) return

        val chestName = InventoryUtils.openInventoryName()
        if (!chestName.startsWith("Your Equipment")) return

        event.makeShiftClick()
    }
}
