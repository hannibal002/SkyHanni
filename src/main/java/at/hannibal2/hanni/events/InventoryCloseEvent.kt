package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.hannimodule.PrimaryFunction

@PrimaryFunction("onInventoryClose")
class InventoryCloseEvent(val inventoryTitle: String, val reopenSameName: Boolean) : HanniEvent()
