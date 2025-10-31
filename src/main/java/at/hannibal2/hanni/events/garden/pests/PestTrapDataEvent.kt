package at.hannibal2.hanni.events.garden.pests

import at.hannibal2.hanni.api.event.HanniEvent

class PestTrapDataEvent(
    val trapsPlaced: Int,
    val fullTraps: Set<Int>,
    val noBaitTraps: Set<Int>,
) : HanniEvent()
