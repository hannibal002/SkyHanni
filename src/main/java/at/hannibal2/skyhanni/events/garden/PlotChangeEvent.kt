package at.hannibal2.hanni.events.garden

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.garden.GardenPlotApi

// When the player moves from one plot to another plot
class PlotChangeEvent(val plot: GardenPlotApi.Plot?) : HanniEvent()
