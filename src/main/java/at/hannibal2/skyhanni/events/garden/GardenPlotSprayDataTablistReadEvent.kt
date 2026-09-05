package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.pests.sprayonator.SprayType
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.utils.SimpleTimeMark

/**
 * Fires whenever a new Spray is read from the tablist.
 * @param plotName the current Plot's name pulled through Player Location not Tablist.
 * @param currentSpray the current known Spray for the plot the player is within.
 * @param newSpray the SprayType read from the tablist.
 * @param newSprayExpiryTime the Expiry time read from tablist
*/
class GardenPlotSprayDataTablistReadEvent(
    val plotName: String,
    val currentSpray: GardenPlotApi.SprayData?,
    val newSpray: SprayType,
    val newSprayExpiryTime: SimpleTimeMark,
) : SkyHanniEvent()
