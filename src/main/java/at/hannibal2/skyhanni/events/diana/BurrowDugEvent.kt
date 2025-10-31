package at.hannibal2.hanni.events.diana

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.LorenzVec

class BurrowDugEvent(val burrowLocation: LorenzVec) : HanniEvent()
