package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MinionCollectLogic {
    private var oldMap = mapOf<String, Int>()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return
        if (!event.inventoryName.contains(" Minion ")) return

        event.inventoryItems[48]?.let {
            if ("§aCollect All" == it.name) {
                openMinion()
            }
        }
    }

    private fun openMinion() {
        if (oldMap.isNotEmpty()) return
        oldMap = count()
    }

    private fun count(): MutableMap<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (stack in InventoryUtils.getItemsInOwnInventory()) {
            val internalName = stack.getInternalName()
            val (newId, amount) = NEUItems.getMultiplier(internalName)
            val old = map[newId] ?: 0
            map[newId] = old + amount * stack.stackSize
        }
        return map
    }

    // hypixel opens a new inventory after clicking on an item in minion inventory, InventoryCloseEvent is not usable here
    @SubscribeEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        closeMinion()
    }

    private fun closeMinion() {
        if (oldMap.isEmpty()) return

        for ((internalId, amount) in count()) {
            val old = oldMap[internalId] ?: 0
            val diff = amount - old

            if (diff > 0) {
                CollectionAPI.addFromInventory(internalId, diff)
            }
        }

        oldMap = emptyMap()
    }
}