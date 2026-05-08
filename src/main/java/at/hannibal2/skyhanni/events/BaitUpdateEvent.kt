package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.BaitType

class BaitUpdateEvent(val currentBait: BaitType?, val currentAmount: Int) : SkyHanniEvent()

