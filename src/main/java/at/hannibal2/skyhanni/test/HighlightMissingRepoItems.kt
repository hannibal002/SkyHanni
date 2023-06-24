package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightMissingRepoItems {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.dev.highlightMissingRepo) return

        val gui = event.gui

        if (gui is GuiChest) {
            highlightItems(gui.inventorySlots.inventorySlots)
        } else if (gui is GuiInventory) {
            val player = Minecraft.getMinecraft().thePlayer
            highlightItems(player.inventoryContainer.inventorySlots)
        }
    }

    private fun highlightItems(slots: Iterable<Slot>) {
        if (NEUItems.allInternalNames.isEmpty()) return
        for (slot in slots) {
            if (!slot.hasStack) continue
            val internalName = slot.stack.getInternalName()
            if (internalName == "") continue
            if (!NEUItems.allInternalNames.contains(internalName)) {
                slot highlight LorenzColor.RED
            }
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: io.github.moulberry.notenoughupdates.events.RepositoryReloadEvent) {
        NEUItems.allItemsCache = NEUItems.readAllNeuItems()
    }
}