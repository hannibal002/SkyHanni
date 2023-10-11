package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ShiftClickEquipment {

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.inventory.shiftClickForEquipment) return

        if (event.gui !is GuiChest) return

        val slot = event.slot ?: return

        if (slot.slotNumber == slot.slotIndex) return

        if (slot.stack == null) return

        val chestName = InventoryUtils.openInventoryName()
        if (!chestName.startsWith("Your Equipment")) return

        event.isCanceled = true
        Minecraft.getMinecraft().playerController.windowClick(
            event.container.windowId,
            event.slot.slotNumber,
            event.clickedButton,
            1,
            Minecraft.getMinecraft().thePlayer
        )
    }
}