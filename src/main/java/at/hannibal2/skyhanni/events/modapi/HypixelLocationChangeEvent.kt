package at.hannibal2.skyhanni.events.modapi

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.HypixelLocation

class HypixelLocationChangeEvent(val location: HypixelLocation) : SkyHanniEvent()
