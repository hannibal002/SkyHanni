package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when the player moves from one [GardenPlot] to another, or leaves all plots.
 *
 * @param plot the plot the player entered, or `null` if the player left all plots.
 */
@PrimaryFunction("onPlotChange")
class PlotChangeEvent(val plot: GardenPlot?) : SkyHanniEvent()
