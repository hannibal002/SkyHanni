package at.hannibal2.skyhanni.events.experiments

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.utils.NeuInternalName

@Thread(RENDER)
class TableXPBottleUsedEvent(val internalName: NeuInternalName, val amount: Int) : SkyHanniEvent()
