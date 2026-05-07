package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.greenhouse
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec

@SkyHanniModule
object SkyMutationsImporter {

    private val config get() = SkyHanniMod.feature.garden.greenhouse

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {


        val plot = GardenPlotApi.getCurrentPlot() ?: return
        if (!plot.greenhouse) return

        4 - 91

        val box = plot.box
        43
        val bottomLeft = LorenzVec(box.minX, 73.0, box.maxZ - 43)
        val data =

    }
}
