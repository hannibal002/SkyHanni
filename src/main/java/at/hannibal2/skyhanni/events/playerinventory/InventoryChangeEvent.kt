package at.hannibal2.skyhanni.events.playerinventory

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.PrimitiveItemStack

/**
 * This event is fired when anything in the inventory changes.
 * It contains the previous and current stacks of items in the inventory.
 *
 * @param oldStacks The previous stacks of items in the inventory.
 * @param newStacks The current stacks of items in the inventory.
 */
class InventoryChangeEvent(val oldStacks: Array<PrimitiveItemStack?>, val newStacks: Array<PrimitiveItemStack?>) : SkyHanniEvent()
