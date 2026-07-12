package at.hannibal2.skyhanni.events.inventory

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread

@Thread(RENDER)
class AttemptedInventoryCloseEvent : CancellableSkyHanniEvent()
