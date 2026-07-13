package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@Thread(NETWORK, RENDER)
@PrimaryFunction("onInventoryClose")
class InventoryCloseEvent(val inventoryTitle: String, val reopenSameName: Boolean) : SkyHanniEvent()
