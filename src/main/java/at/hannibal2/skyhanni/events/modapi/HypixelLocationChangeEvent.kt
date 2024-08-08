package at.hannibal2.skyhanni.events.modapi

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.hypixel.location.HypixelLocation

class HypixelLocationChangeEvent(val location: HypixelLocation) : SkyHanniEvent()
