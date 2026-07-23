package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when an inventory is closed.
 *
 * @param inventoryTitle the title of the closed inventory
 * @param reopenSameName whether the same inventory was immediately reopened
 */
@Thread(NETWORK, RENDER)
@PrimaryFunction("onInventoryClose")
class InventoryCloseEvent(val inventoryTitle: String, val reopenSameName: Boolean) : SkyHanniEvent()
