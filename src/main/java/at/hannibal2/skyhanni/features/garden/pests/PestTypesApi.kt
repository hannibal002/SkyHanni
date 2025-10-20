package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isPestCountInaccurate
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.pests
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object PestTypesApi {

    var pests = mutableMapOf<GardenPlotApi.Plot, List<PestType>>()

    private var lastCheckedPlot = 0

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onMobEventSpawned(event: MobEvent.Spawn) {
        val type = PestType.getByNameOrNull(event.mob.name) ?: return
        val plot = GardenPlotApi.plots.find { it.box.isVecInside(event.mob.centerCords.toVec3()) } ?: return
        if (lastCheckedPlot != plot.id) pests[plot] = listOf()
        if (plot.pests >= 1 && !plot.isPestCountInaccurate && (pests.get(plot)?.size ?: 0) == plot.pests) return

        pests.addToPlot(plot, type)
        lastCheckedPlot = plot.id
    }

    @HandleEvent
    fun onPestKilled(event: PestKillEvent) {
        pests.removeFromPlot(event.plot, event.type)
    }

    private fun MutableMap<GardenPlotApi.Plot, List<PestType>>.addToPlot(plot: GardenPlotApi.Plot, pestType: PestType) {
        this[plot] = this.getOrDefault(plot, emptyList()) + pestType
    }

    private fun MutableMap<GardenPlotApi.Plot, List<PestType>>.removeFromPlot(plot: GardenPlotApi.Plot, pestType: PestType) {
        val currentList = this[plot].orEmpty()
        val indexToRemove = currentList.indexOfFirst { it == pestType }
        if (indexToRemove != -1) {
            this[plot] = currentList.filterIndexed { index, _ -> index != indexToRemove }
        }
    }
}
