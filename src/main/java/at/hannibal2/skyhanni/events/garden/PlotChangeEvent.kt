package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.plot.GardenPlot

/**
 * Fired when the player moves from one [at.hannibal2.skyhanni.features.garden.plot.GardenPlot] to another, or leaves all plots.
 *
 * @param plot the plot the player entered, or `null` if the player left all plots.
 */
class PlotChangeEvent(val plot: GardenPlot?) : SkyHanniEvent()
