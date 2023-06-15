package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkyBlockLevelGuideHelper {

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.inventory.highlightMissingSkyBlockLevelGuide) return

        if (InventoryUtils.openInventoryName().contains("Guide ➜")) {

            if (event.gui !is GuiChest) return
            val guiChest = event.gui
            val chest = guiChest.inventorySlots as ContainerChest

            for (slot in chest.inventorySlots) {
                if (slot == null) continue
                if (slot.slotNumber != slot.slotIndex) continue
                val name = slot.stack?.name ?: continue

                if (name.startsWith("§c✖")) {
                    slot highlight LorenzColor.RED
                }
            }
        }
    }
}