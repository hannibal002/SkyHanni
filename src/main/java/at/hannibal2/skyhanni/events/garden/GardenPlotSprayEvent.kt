package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.pests.SprayType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onGardenPlotSprayChanged")
sealed class GardenPlotSprayEvent(val plot: GardenPlotApi.Plot?, val type: SprayType): SkyHanniEvent() {

    @PrimaryFunction("onGardenPlotSprayAddedEvent")
    class GardenPlotSprayAddedEvent(plot: GardenPlotApi.Plot?, type: SprayType): GardenPlotSprayEvent(plot, type)

    @PrimaryFunction("onGardenPlotSprayExpiredEvent")
    class GardenPlotSprayExpiredEvent(plot: GardenPlotApi.Plot?, type: SprayType): GardenPlotSprayEvent(plot, type)
}

