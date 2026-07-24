package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when a sack inventory is opened.
 *
 * @param isNewInventory whether this is a different sack than the previously opened one
 * @param inventoryOpenEvent the underlying inventory open event
 */
@PrimaryFunction("onSackOpen")
class SackOpenEvent(val isNewInventory: Boolean, val inventoryOpenEvent: InventoryFullyOpenedEvent) : SkyHanniEvent()
