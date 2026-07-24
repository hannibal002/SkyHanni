package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.pests.SprayType
import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onGardenPlotSprayChanged")
sealed class GardenPlotSprayEvent(val plot: GardenPlot?, val type: SprayType) : SkyHanniEvent() {

    @PrimaryFunction("onGardenPlotSprayAddedEvent")
    class GardenPlotSprayAddedEvent(plot: GardenPlot?, type: SprayType, val amount: Int) : GardenPlotSprayEvent(plot, type)

    @PrimaryFunction("onGardenPlotSprayExpiredEvent")
    class GardenPlotSprayExpiredEvent(plot: GardenPlot?, type: SprayType) : GardenPlotSprayEvent(plot, type)
}

