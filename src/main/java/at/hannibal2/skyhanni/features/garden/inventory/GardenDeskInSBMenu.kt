package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenDeskInSBMenu {

    private val config get() = SkyHanniMod.feature.garden
    private var showItem = false

    private val item by lazy {
        var neuItem = NEUItems.getItemStackOrNull("DOUBLE_PLANT")
        if (neuItem == null) {
            neuItem = ItemStack(Blocks.double_plant)
            Utils.showOutdatedRepoNotification()
        }
        Utils.createItemStack(neuItem!!.item, "§bDesk", "§7Click here to", "§7run §e/desk")
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
        if (event.inventory is ContainerLocalMenu && showItem && event.slotNumber == 10) {
            event.replaceWith(item)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (showItem && event.slotId == 10) {
            event.isCanceled = true
            LorenzUtils.sendCommandToServer("desk")
        }
    }
}