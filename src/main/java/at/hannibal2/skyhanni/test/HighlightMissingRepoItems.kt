package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.MultiFilter
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightMissingRepoItems {
    private val ignoreItems = MultiFilter()

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
            val internalName = slot.stack?.getInternalNameOrNull() ?: continue
            if (NEUItems.allInternalNames.contains(internalName)) continue
            if (ignoreItems.match(internalName.asString())) continue

            slot highlight LorenzColor.RED
        }
    }

    @SubscribeEvent
    fun onNeuRepoReload(event: io.github.moulberry.notenoughupdates.events.RepositoryReloadEvent) {
        NEUItems.allItemsCache = NEUItems.readAllNeuItems()
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        event.getConstant("IgnoredItems")?.let {
            ignoreItems.load(it.asJsonObject)
        }
    }
}