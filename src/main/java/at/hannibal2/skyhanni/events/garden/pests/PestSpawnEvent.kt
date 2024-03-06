package at.hannibal2.skyhanni.events.garden.pests

import at.hannibal2.skyhanni.events.LorenzEvent

class PestSpawnEvent(val amountPests: Int, val plotName: String) : LorenzEvent()
