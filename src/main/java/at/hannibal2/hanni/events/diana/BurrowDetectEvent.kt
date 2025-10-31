package at.hannibal2.hanni.events.diana

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.event.diana.BurrowType
import at.hannibal2.hanni.utils.LorenzVec

class BurrowDetectEvent(val burrowLocation: LorenzVec, val type: BurrowType) : HanniEvent()
