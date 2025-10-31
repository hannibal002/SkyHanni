package at.hannibal2.hanni.events.experiments

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.NeuInternalName

class TableXPBottleUsedEvent(val internalName: NeuInternalName, val amount: Int) : HanniEvent()
