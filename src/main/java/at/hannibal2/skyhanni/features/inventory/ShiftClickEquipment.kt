package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ShiftClickEquipment {

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (event.gui !is GuiChest) return

        val chestName = InventoryUtils.openInventoryName()

        val slot = event.slot ?: return

        if (slot.slotNumber == slot.slotIndex) return

        if (slot.stack == null) return

        if (SkyHanniMod.feature.inventory.shiftClickForEquipment && chestName.startsWith("Your Equipment")) {
            Minecraft.getMinecraft().playerController.windowClick(
                event.container.windowId,
                event.slot.slotNumber,
                event.clickedButton,
                1,
                Minecraft.getMinecraft().thePlayer
            )
            event.isCanceled = true
        }
    }
}