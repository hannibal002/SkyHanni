package at.hannibal2.skyhanni.events.modapi

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.hypixel.location.HypixelLocation

/**
 * This event is emitted whenever a player joins Hypixel or changes servers on Hypixel.
 *
 */
class HypixelLocationChangeEvent(
    val location: HypixelLocation,
    val previousLocation: HypixelLocation?
) : SkyHanniEvent() {
    private fun justJoinedHypixel() = previousLocation != null
}

