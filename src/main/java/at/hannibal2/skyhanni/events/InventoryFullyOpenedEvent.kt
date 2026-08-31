package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.toPrimitiveStackOrNull
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty

/**
 * Sealed base class for inventory events that provide access to the current inventory's slot data.
 *
 * These events are posted from the packet handler, so handlers do not run on the main thread.
 * Anything that measures text width, such as building renderables, has to go through
 * DelayedRun.runOrNextTick.
 *
 * @see InventoryFullyOpenedEvent
 * @see InventoryUpdatedEvent
 */
sealed class InventoryOpenEvent(private val inventory: OtherInventoryData.Inventory) : SkyHanniEvent() {

    val inventoryId: Int get() = inventory.windowId
    val inventoryName: String get() = inventory.title
    val inventorySize: Int get() = inventory.slotCount
    val inventoryItems: Map<Int, SafeItemStack> = inventory.items.filterValues { it.isNotEmpty() }
    val inventoryItemsWithNull: Map<Int, SafeItemStack?> by lazy {
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
 * This event is fired after every slot in the newly opened inventory has item data.
 *
 * New inventory data first gets sent as an empty inventory from the server.
 * Item stack slot information is sent afterwards, sometimes with a short delay.
 *
 * This approach is faster than waiting a fixed duration after the inventory open packet is detected.
 *
 * Since this logic only works via packets, and the player inventory (pressing E) is client side,
 * this event does not get fired when opening the inventory via pressing E.
 *
 * TODO does not work for inventories with empty slots (e.g. "Teleport to Player" ghost ability in dungeons).
 */
@PrimaryFunction("onInventoryFullyOpened")
class InventoryFullyOpenedEvent(inventory: OtherInventoryData.Inventory) : InventoryOpenEvent(inventory)

/**
 * Fired whenever the slot data of the currently open inventory changes.
 *
 * This fires once immediately after [InventoryFullyOpenedEvent] on initial open,
 * and again on any subsequent slot update while the inventory remains open.
 * Updates after the initial open are delayed by one tick and therefore run on the main thread,
 * only the initial post comes from the packet handler.
 *
 * An update still pending when the inventory closes is dropped, so this never fires after the
 * [InventoryCloseEvent] of that inventory.
 */
@PrimaryFunction("onInventoryUpdated")
class InventoryUpdatedEvent(inventory: OtherInventoryData.Inventory) : InventoryOpenEvent(inventory)
