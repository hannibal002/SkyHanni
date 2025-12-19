package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.makeShiftClick
import net.minecraft.client.gui.screens.inventory.ContainerScreen

@SkyHanniModule
object ShiftClickEquipment {

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!SkyHanniMod.feature.inventory.shiftClickForEquipment) return

        if (event.gui !is ContainerScreen) return

        val slot = event.slot ?: return

        if (slot.index == slot.containerSlot) return

        if (slot.item == null) return

        val chestName = InventoryUtils.openInventoryName()
        if (!chestName.startsWith("Your Equipment")) return

        event.makeShiftClick()
    }
}
