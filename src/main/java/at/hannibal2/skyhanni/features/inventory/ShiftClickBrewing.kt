package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.InventoryUtils.makeShiftClick
import net.minecraft.client.gui.inventory.GuiChest

@HanniModule
object ShiftClickBrewing {
    private const val closeButtonIndex = 49

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!HanniMod.feature.inventory.shiftClickBrewing) return

        if (event.gui !is GuiChest) return

        if (event.slot == null || event.slotId == closeButtonIndex) return

        val chestName = InventoryUtils.openInventoryName()
        if (!chestName.startsWith("Brewing Stand")) return

        event.makeShiftClick()
    }
}
