package at.hannibal2.skyhanni.events.farming

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLane

class FarmingLaneSwitchEvent(val lane: FarmingLane?) : LorenzEvent()
