package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.NeuInternalName

class ItemAddInInventoryEvent(val internalName: NeuInternalName, val amount: Int) : HanniEvent()
