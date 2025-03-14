package at.hannibal2.skyhanni.events.garden.pests

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class PestTrapDataEvent(
    val trapsPlaced: Int,
    val anyFull: Boolean,
    val anyNoBait: Boolean,
) : SkyHanniEvent()
