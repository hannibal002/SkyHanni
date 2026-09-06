package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.gui.screens.inventory.ContainerScreen

@SkyHanniModule
object ShiftClickEquipment {
    @HandleEvent(onlyOnSkyblock = true)
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!SkyHanniMod.feature.inventory.shiftClickForEquipment) return

        if (event.gui !is ContainerScreen) return

        val slot = event.slot ?: return

        if (slot.index == slot.containerSlot) return

        if (slot.item.isEmpty) return
        if (!CurrentEquipmentApi.inventory.isInside()) return

        event.makeShiftClick()
    }
}
