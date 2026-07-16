package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onSackOpen")
class SackOpenEvent(val isNewInventory: Boolean, val inventoryOpenEvent: InventoryFullyOpenedEvent) : SkyHanniEvent()
