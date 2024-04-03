package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.toPrimitiveStackOrNull
import net.minecraft.item.ItemStack

open class InventoryOpenEvent(private val inventory: OtherInventoryData.Inventory) : LorenzEvent() {

    val inventoryId: Int by lazy { inventory.windowId }
    val inventoryName: String by lazy { inventory.title }
    val inventorySize: Int by lazy { inventory.slotCount }
    val inventoryItems: Map<Int, ItemStack> by lazy { inventory.items }
    val inventoryItemsWithNull: Map<Int, ItemStack?> by lazy {
        (0 until inventorySize).associateWith { inventoryItems[it] }
    }
    val inventoryItemsPrimitive: Map<Int, PrimitiveItemStack> by lazy {
        val map = mutableMapOf<Int, PrimitiveItemStack>()
        for ((slot, item) in inventoryItems) {
            item.toPrimitiveStackOrNull()?.let {
                map[slot] = it
            }
        }
        map
    }
    val fullyOpenedOnce: Boolean get() = inventory.fullyOpenedOnce
}

class InventoryFullyOpenedEvent(inventory: OtherInventoryData.Inventory) : InventoryOpenEvent(inventory)

class InventoryUpdatedEvent(inventory: OtherInventoryData.Inventory) : InventoryOpenEvent(inventory)
