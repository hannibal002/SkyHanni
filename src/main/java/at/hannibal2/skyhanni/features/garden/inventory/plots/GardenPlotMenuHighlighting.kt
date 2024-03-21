package at.hannibal2.skyhanni.features.garden.inventory.plots

import at.hannibal2.skyhanni.config.features.garden.PlotMenuHighlightingConfig.PlotStatusTypes
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.currentSpray
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.pests
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.plots
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenPlotMenuHighlighting {

    private val config get() = GardenAPI.config.plotMenuHighlighting

    private var highlightedPlots = mutableListOf<Pair<GardenPlotAPI.Plot, PlotStatusTypes>>()

    private fun isHighlighted(plot: GardenPlotAPI.Plot) = highlightedPlots.any { it.first == plot }
    private fun isCurrentHighlight(plot: GardenPlotAPI.Plot, current: PlotStatusTypes) =
        highlightedPlots.any { it.first == plot && it.second == current }

    private fun handleStackSize(plot: GardenPlotAPI.Plot, status: PlotStatusTypes): Int {
        return when (status.name) {
            "§cPests" -> return plot.pests
            "§eSprays" -> return plot.currentSpray?.expiry?.timeUntil()?.inWholeMinutes?.toInt() ?: 1
            else -> 1
        }
    }

    private fun handleCurrent(plot: GardenPlotAPI.Plot, status: PlotStatusTypes) {
        val isHighlighted = isHighlighted(plot)
        val isCurrent = isCurrentHighlight(plot, status)
        if (!isHighlighted || isCurrent) {
            if (!isHighlighted) highlightedPlots.add(plot to status)
        } else {
            highlightedPlots.replaceAll { if (it.first == plot) plot to status else it }
        }
    }

    private fun getLowestIndexItem(array: MutableList<PlotStatusTypes>): Int? {
        var lowest = 999
        array.forEach { status ->
            val foundStatus = config.deskPlotStatusTypes.find { it == status }
            if (foundStatus != null) {
                val index = config.deskPlotStatusTypes.indexOf(foundStatus)
                if (index < lowest) lowest = index
            }
        }
        return if(lowest == 999) null else lowest
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val list = mutableListOf<PlotStatusTypes>()
            val plot = plots.find { it.inventorySlot == slot.slotIndex } ?: continue
            if (plot.pests >= 1) list.add(PlotStatusTypes.PESTS)
            if (plot.currentSpray != null) list.add(PlotStatusTypes.SPRAYS)
            if (!plot.unlocked) list.add(PlotStatusTypes.LOCKED)
            if (plot == GardenPlotAPI.getCurrentPlot()) list.add(PlotStatusTypes.CURRENT)

            getLowestIndexItem(list)?.let { index ->
                val status = config.deskPlotStatusTypes[index]
                handleCurrent(plot, status)
            } ?: highlightedPlots.removeIf { it.first == plot }
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled() || highlightedPlots.size <= 0) return

        for (plot in highlightedPlots) {
            val slot = InventoryUtils.getItemsInOpenChest()[plot.first.inventorySlot] ?: continue
            slot.stack.stackSize = handleStackSize(plot.first, plot.second)
            slot highlight plot.second.highlightColor
        }
    }

    private fun isEnabled() =
        GardenAPI.inGarden() && InventoryUtils.openInventoryName() == "Configure Plots" && config.enabled
}
