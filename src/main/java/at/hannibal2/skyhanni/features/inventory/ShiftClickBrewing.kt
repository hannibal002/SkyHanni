package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.makeShiftClick
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen

@SkyHanniModule
object ShiftClickBrewing {
    private const val closeButtonIndex = 49

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!SkyHanniMod.feature.inventory.shiftClickBrewing) return

        if (event.gui !is GenericContainerScreen) return

        if (event.slot == null || event.slotId == closeButtonIndex) return

        val chestName = InventoryUtils.openInventoryName()
        if (!chestName.startsWith("Brewing Stand")) return

        event.makeShiftClick()
    }
}
