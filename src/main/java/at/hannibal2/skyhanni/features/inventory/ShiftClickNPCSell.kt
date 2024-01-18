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

object ShiftClickNPCSell {

    private val config get() = SkyHanniMod.feature.inventory.shiftClickNPCSell

    private val sellSlot = 49
    private val lastLoreLineOfSellPattern by RepoPattern.pattern(
        "inventory.npc.sell.lore",
        "§7them to this Shop!|§eClick to buyback!"
    )

    var inInventory = false
        private set

    fun isEnabled() = LorenzUtils.inSkyBlock && config

    @SubscribeEvent
    fun onOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        inInventory = lastLoreLineOfSellPattern.matches(event.inventoryItems[sellSlot]?.getLore()?.lastOrNull())
    }

    @SubscribeEvent
    fun onClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        val slot = event.slot ?: return

        if (slot.slotNumber == slot.slotIndex) return

        event.isCanceled = true
        Minecraft.getMinecraft().playerController.windowClick(
            event.container.windowId, event.slot.slotNumber, event.clickedButton, 1, Minecraft.getMinecraft().thePlayer
        )
    }
}
