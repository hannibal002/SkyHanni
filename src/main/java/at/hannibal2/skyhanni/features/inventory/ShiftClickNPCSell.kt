package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ShiftClickNPCSell {

    val config get() = SkyHanniMod.feature.inventory.shiftClickNPCSell

    val sellSlot = 49
    val lastLoreLineOfSell by RepoPattern.pattern("inventory.npc.sell.lore", "§7them to this Shop!|§eClick to buyback!")

    var isInNPCSell = false

    fun enable() = LorenzUtils.inSkyBlock && config

    @SubscribeEvent
    fun onOpen(event: InventoryFullyOpenedEvent) {
        if (!enable()) return
        isInNPCSell = lastLoreLineOfSell.matches(event.inventoryItems[sellSlot]?.getLore()?.lastOrNull() ?: "")
    }

    @SubscribeEvent
    fun onClose(event: InventoryCloseEvent) {
        isInNPCSell = false
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!enable()) return
        if (!isInNPCSell) return

        val slot = event.slot ?: return

        if (slot.slotNumber == slot.slotIndex) return

        event.isCanceled = true
        Minecraft.getMinecraft().playerController.windowClick(
            event.container.windowId, event.slot.slotNumber, event.clickedButton, 1, Minecraft.getMinecraft().thePlayer
        )
    }
}
