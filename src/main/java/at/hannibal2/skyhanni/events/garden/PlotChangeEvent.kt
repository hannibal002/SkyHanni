package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi

class PlotChangeEvent(val previousPlot: GardenPlotApi.Plot?, val newPlot: GardenPlotApi.Plot?) : SkyHanniEvent()
