package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.OtherInventoryData
import at.hannibal2.hanni.hannimodule.PrimaryFunction
import at.hannibal2.hanni.utils.PrimitiveItemStack
import at.hannibal2.hanni.utils.PrimitiveItemStack.Companion.toPrimitiveStackOrNull
import at.hannibal2.hanni.utils.compat.InventoryCompat.isNotEmpty
import net.minecraft.item.ItemStack

open class InventoryOpenEvent(private val inventory: OtherInventoryData.Inventory) : HanniEvent() {

    val inventoryId: Int get() = inventory.windowId
    val inventoryName: String get() = inventory.title
    val inventorySize: Int get() = inventory.slotCount
    val inventoryItems: Map<Int, ItemStack> get() {
        val items = inventory.items
        items.entries.removeIf { !it.value.isNotEmpty() }
        return items
    }
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

/**
 * This event is getting fired after every slot in the newly opened inventory has item data.
 *
 * New inventory data gets first sent as an empty inventory from the server.
 * Item stack slot information is sent afterwards, sometimes with a short delay.
 *
 * This approach is faster than to wait a fix duration after the inventory open packet is detected.
 *
 * Since this logic only works via packets, and the player inventory (pressing E) is client side,
 * this event does not get fired when opening the inventory via pressing E.
 *
 * TODO does not work for inventories with empty slots. e.g. dungeon when death ghost tp menu "Teleport to Player".
 */
@PrimaryFunction("onInventoryFullyOpened")
class InventoryFullyOpenedEvent(inventory: OtherInventoryData.Inventory) : InventoryOpenEvent(inventory)

class InventoryUpdatedEvent(inventory: OtherInventoryData.Inventory) : InventoryOpenEvent(inventory)
