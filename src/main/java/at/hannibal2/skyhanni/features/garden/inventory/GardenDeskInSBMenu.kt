package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenDeskInSBMenu {

    private val config get() = GardenAPI.config
    private var showItem = false

    private val item by lazy {
        val neuItem = "DOUBLE_PLANT".asInternalName().getItemStack()
        Utils.createItemStack(
            neuItem.item,
            "§bOpen Desk",
            "§8(From SkyHanni)",
            "",
            "§7Click here to",
            "§7run §e/desk"
        )
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        showItem = GardenAPI.inGarden() && config.deskInSkyBlockMenu && event.inventoryName == "SkyBlock Menu"
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        showItem = false
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (event.inventory is ContainerLocalMenu && showItem && event.slotNumber == 10) {
            event.replaceWith(item)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (showItem && event.slotId == 10) {
            event.isCanceled = true
            ChatUtils.sendCommandToServer("desk")
        }
    }
}
