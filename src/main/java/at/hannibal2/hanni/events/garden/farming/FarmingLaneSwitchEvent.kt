package at.hannibal2.hanni.events.garden.farming

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.garden.farming.lane.FarmingLane

class FarmingLaneSwitchEvent(val lane: FarmingLane?) : HanniEvent()
