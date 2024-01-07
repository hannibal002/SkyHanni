package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.OtherInventoryData
import net.minecraft.item.ItemStack

open class InventoryOpenEvent(private val inventory: OtherInventoryData.Inventory) : LorenzEvent() {
    val inventoryId: Int by lazy { inventory.windowId }
    val inventoryName: String by lazy { inventory.title }
    val inventorySize: Int by lazy { inventory.slotCount }
    val inventoryItems: Map<Int, ItemStack> by lazy { inventory.items }
    val fullyOpenedOnce: Boolean get() = inventory.fullyOpenedOnce
}

class InventoryFullyOpenedEvent(inventory: OtherInventoryData.Inventory) : InventoryOpenEvent(inventory)

class InventoryUpdatedEvent(inventory: OtherInventoryData.Inventory) : InventoryOpenEvent(inventory)
