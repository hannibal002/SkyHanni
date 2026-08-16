package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.pests.sprayonator.SprayType
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.utils.SimpleTimeMark

class GardenPlotSprayDataTablistReadEvent(
    val plotName: String,
    val currentSpray: GardenPlotApi.SprayData?,
    val newSpray: SprayType,
    val newSprayExpiryTime: SimpleTimeMark,
) : SkyHanniEvent()
