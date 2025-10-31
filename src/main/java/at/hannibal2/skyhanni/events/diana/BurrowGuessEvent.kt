package at.hannibal2.hanni.events.diana

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.LorenzVec

class BurrowGuessEvent(val guessLocation: LorenzVec, val precise: Boolean, val new: Boolean) : HanniEvent()
