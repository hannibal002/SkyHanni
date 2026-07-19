package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi

/**
 * Fired when the player moves from one [GardenPlotApi.Plot] to another, or leaves all plots.
 *
 * @param plot the plot the player entered, or `null` if the player left all plots.
 */
class PlotChangeEvent(val plot: GardenPlotApi.Plot?) : SkyHanniEvent()
