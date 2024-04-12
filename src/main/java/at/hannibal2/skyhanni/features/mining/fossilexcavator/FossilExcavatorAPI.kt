package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FossilExcavatorAPI {

    var inInventory = false
    var inExcavatorMenu = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!IslandType.DWARVEN_MINES.isInIsland()) return
        if (event.inventoryName != "Fossil Excavator") return
        inInventory = true
        val slots = InventoryUtils.getItemsInOpenChest()
        val itemNames = slots.map { it.stack.displayName.removeColor() }
        inExcavatorMenu = itemNames.any { it == "Start Excavator" }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        inInventory = false
        inExcavatorMenu = false
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        inExcavatorMenu = false
    }
}
