package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.HotmAPI
import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class PowderGainEvent(val powder: HotmAPI.Powder, val amount: Long) : SkyHanniEvent()
