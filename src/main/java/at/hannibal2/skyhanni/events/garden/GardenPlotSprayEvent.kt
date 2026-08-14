package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.pests.SprayType
import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fires whenever a sub event (GardenPlotSprayAddedEvent & GardenPlotSprayExpiredEvent) fires
 * @param plot GardenPlot that the spray status has changed on.
 * @param type is the SprayType that has changed status.
 */
@PrimaryFunction("onGardenPlotSprayChanged")
sealed class GardenPlotSprayEvent(val plot: GardenPlot, val type: SprayType) : SkyHanniEvent() {

    /**
     * Fired from GardenPlotApi when the plotSprayedPattern matches a chat message.
     * @param plot is the GardenPlot that has the spray added.
     * @param type is the SprayType that has been added.
     * @param amount is the amount of the SprayType that was used.
     */
    @PrimaryFunction("onGardenPlotSprayAdded")
    class SprayAddedEvent(plot: GardenPlot, type: SprayType, val amount: Int) : GardenPlotSprayEvent(plot, type)

    /**
     * Fired from GardenPlotApi when the plotSprayExpiredPattern matches a chat message.
     * @param plot is the GardenPlot that the spray has expired on.
     * @param type is the SprayType that has expired.
     */
    @PrimaryFunction("onGardenPlotSprayExpired")
    class SprayExpiredEvent(plot: GardenPlot, type: SprayType) : GardenPlotSprayEvent(plot, type)
}
