package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.MiningEventsApi

sealed class MiningEventEvent(val event: MiningEventsApi.MiningEvent) : SkyHanniEvent() {
    class Started(event: MiningEventsApi.MiningEvent) : MiningEventEvent(event)
    class Ended(event: MiningEventsApi.MiningEvent) : MiningEventEvent(event)
}

