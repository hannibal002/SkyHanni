package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
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

class FandomWikiFromMenus {
    private var inInventory = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
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
        val itemInHand = InventoryUtils.getItemInHand() ?: return
        val itemInHandName = itemInHand.nameWithEnchantment ?: return
        val internalName = itemInHand.getInternalName().asString() ?: return

        if ((itemInHandName == "") || (event.slotId == 11 && itemName.contains("Wiki Command") && chestName.contains("Wiki"))) {
            LorenzUtils.clickableChat("§e[SkyHanni] Click here to visit the Hypixel Skyblock Fandom Wiki!", "wiki")
        } else if (event.slotId == 15 && itemName.contains("Wikithis Command") && chestName.contains("Wiki")) {
            LorenzUtils.clickableChat("§e[SkyHanni] Click here to search for the $itemInHandName §eon the Hypixel Skyblock Fandom Wiki!", "wiki $internalName")
        }
        event.isCanceled = true

    }
    private fun isEnabled() = SkyHanniMod.feature.commands.useFandomWiki
}