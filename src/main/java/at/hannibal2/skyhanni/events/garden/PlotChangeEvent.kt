package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi

// When the player moves from one plot to another plot
class PlotChangeEvent(val plot: GardenPlotApi.Plot?) : SkyHanniEvent()
