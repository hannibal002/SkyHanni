package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.ArrowType

class QuiverUpdateEvent(val currentArrow: ArrowType?, val currentAmount: Int) : HanniEvent()
