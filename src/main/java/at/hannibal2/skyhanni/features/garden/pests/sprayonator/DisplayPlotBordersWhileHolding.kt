package at.hannibal2.skyhanni.features.garden.pests.sprayonator

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi.renderPlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor

@SkyHanniModule
object DisplayPlotBordersWhileHolding {

    private val config get() = PestApi.config.spray

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.drawPlotsBorderWhenInHands) return
        if (!PestApi.hasSprayonatorInHand()) return
        val plot = GardenPlotApi.currentPlot ?: return
        event.renderPlot(plot, LorenzColor.YELLOW.toColor(), LorenzColor.DARK_BLUE.toColor())
    }
}
