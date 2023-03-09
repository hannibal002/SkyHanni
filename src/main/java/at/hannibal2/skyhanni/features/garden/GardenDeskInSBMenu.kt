package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.Garden
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.NEUItems
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenDeskInSBMenu {

    private val config: Garden get() = SkyHanniMod.feature.garden
    private var showItem = false

    private val item by lazy {
        val neuItem = NEUItems.getItemStack("DOUBLE_PLANT")
        Utils.createItemStack(neuItem.item, "§bDesk", "§7Click here to", "§7run §e/desk")
    }


    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        showItem = GardenAPI.inGarden() && config.deskInSkyBlockMenu && event.inventoryName == "SkyBlock Menu"
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        showItem = false
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (showItem && event.slotNumber == 10) {
            event.replaceWith(item)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (showItem && event.slotId == 10) {
            event.isCanceled =  true
            val thePlayer = Minecraft.getMinecraft().thePlayer
            thePlayer.sendChatMessage("/desk")
        }
    }
}