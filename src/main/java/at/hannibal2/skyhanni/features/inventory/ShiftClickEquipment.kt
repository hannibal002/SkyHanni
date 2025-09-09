package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.makeShiftClick
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen

@SkyHanniModule
object ShiftClickEquipment {

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!SkyHanniMod.feature.inventory.shiftClickForEquipment) return

        if (event.gui !is GenericContainerScreen) return

        val slot = event.slot ?: return

        if (slot.id == slot.index) return

        if (slot.stack == null) return

        val chestName = InventoryUtils.openInventoryName()
        if (!chestName.startsWith("Your Equipment")) return

        event.makeShiftClick()
    }
}
