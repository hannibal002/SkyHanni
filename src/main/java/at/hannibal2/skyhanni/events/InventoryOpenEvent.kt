package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.OtherInventoryData
import net.minecraft.item.ItemStack

open class BaseInventoryOpenEvent(inventory: OtherInventoryData.Inventory): LorenzEvent() {
    val inventoryId: Int by lazy { inventory.windowId }
    val inventoryName: String by lazy {inventory.title }
    val inventorySize: Int by lazy {inventory.slotCount }
    val inventoryItems: MutableMap<Int, ItemStack>  by lazy {inventory.items }
}

class InventoryOpenEvent(inventory: OtherInventoryData.Inventory): BaseInventoryOpenEvent(inventory)

// Firing with items that are added later
class LateInventoryOpenEvent(inventory: OtherInventoryData.Inventory): BaseInventoryOpenEvent(inventory)