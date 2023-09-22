package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FandomWikiIsSoMuchBetter {
    private var inInventory = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inInventory = false
        if (!event.inventoryName.contains("Wiki")) return
        inInventory = true
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: SlotClickEvent) {

        if (!inInventory || !isEnabled()) return

        val chestName = InventoryUtils.openInventoryName()
        val itemName = event.slot.stack.displayName
        val itemInHand = InventoryUtils.getItemInHand()?.name ?: ""

        if ((itemInHand == "") || (event.slotId == 11 && itemName.contains("Wiki Command") && chestName.contains("Wiki"))) {
            LorenzUtils.clickableChat("§e[SkyHanni] Click here to visit the Hypixel Skyblock Fandom Wiki!", "shwiki")
        } else if (event.slotId == 15 && itemName.contains("Wikithis Command") && chestName.contains("Wiki")) {
            LorenzUtils.chat("${itemInHand.removeColor()}")
            LorenzUtils.clickableChat("§e[SkyHanni] Click here to search for the $itemInHand §eon the Hypixel Skyblock Fandom Wiki!", "shwiki ${itemInHand.removeColor()}")
        }
        event.isCanceled = true

    }
    private fun isEnabled() = SkyHanniMod.feature.commands.useFandomWiki
}