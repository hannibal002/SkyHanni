package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MiddleClickInTrade {
    private val config get() = SkyHanniMod.feature.inventory.middleClickInTrade

    private var inTradeMenu = false

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config || !inTradeMenu) return
        if(event.slot == null) return
        // only allow clicks in inventory and rebuy slots
        if(event.slotId < 49) return
        event.makePickblock()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!config || event.inventoryName != "Trades") return
        inTradeMenu = true
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        clearData()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        clearData()
    }

    private fun clearData() {
        inTradeMenu = false
    }
}
