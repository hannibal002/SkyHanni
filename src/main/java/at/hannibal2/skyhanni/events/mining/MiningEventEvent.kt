package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.MiningEventsApi
import kotlin.time.Duration

open class MiningEventEvent(val event: MiningEventsApi.MiningEvent) : SkyHanniEvent() {
    class Started(event: MiningEventsApi.MiningEvent, duration: Duration) : MiningEventEvent(event)
    class Ended(event: MiningEventsApi.MiningEvent) : MiningEventEvent(event)
}

