package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.OtherInventoryData
import net.minecraft.item.ItemStack

open class InventoryEvent(inventory: OtherInventoryData.Inventory): LorenzEvent() {
    val inventoryId: Int by lazy { inventory.windowId }
    val inventoryName: String by lazy {inventory.title }
    val inventorySize: Int by lazy {inventory.slotCount }
    val inventoryItems: Map<Int, ItemStack>  by lazy {inventory.items }
}

class InventoryOpenEvent(inventory: OtherInventoryData.Inventory): InventoryEvent(inventory)

// Firing with items that are added later
class LateInventoryOpenEvent(inventory: OtherInventoryData.Inventory): InventoryEvent(inventory)