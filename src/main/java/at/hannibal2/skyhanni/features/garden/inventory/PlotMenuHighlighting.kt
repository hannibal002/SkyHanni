package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.config.features.garden.PlotMenuHighlightingConfig.PlotStatusTypes
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.currentSpray
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.pests
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.plots
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlotMenuHighlighting {

    private val config get() = GardenAPI.config.plotMenuHighlighting

    private var highlightedPlots: MutableList<Pair<GardenPlotAPI.Plot, String>> = mutableListOf()

    private fun isHighlighted(plot: GardenPlotAPI.Plot) = highlightedPlots.any { it.first == plot }
    private fun isCurrentHighlight(plot: GardenPlotAPI.Plot, current: String) =
        highlightedPlots.any { it.first == plot && it.second == current }

    private fun handleCurrent(slot: Slot, plot: GardenPlotAPI.Plot, status: PlotStatusTypes) {
        val isHighlighted = isHighlighted(plot)
        val isCurrent = isCurrentHighlight(plot, status.name)
        if (!isHighlighted || isCurrent) {
            when (status.name) {
                "§cPests" -> slot.stack.stackSize =
                    plot.pests

                "§eSprays" -> slot.stack.stackSize =
                    plot.currentSpray?.expiry?.timeUntil()?.inWholeMinutes?.toInt() ?: 1
            }
            slot highlight status.highlightColor
            if (!isHighlighted) highlightedPlots.add(plot to status.name)
        } else {
            when (status.name) {
                "§cPests" -> slot.stack.stackSize =
                    plot.pests

                "§eSprays" -> slot.stack.stackSize =
                    plot.currentSpray?.expiry?.timeUntil()?.inWholeMinutes?.toInt() ?: 1
            }
            slot highlight status.highlightColor
            highlightedPlots.replaceAll { if (it.first == plot) plot to status.name else it }
        }
    }

    private fun getLowestIndexItem(array: MutableList<String>): Int {
        var lowest = if (array.size != 0) 999 else return 999
        array.forEach { name ->
            val foundStatus = config.deskPlotStatusTypes.find { it.name == name }
            if (foundStatus != null) {
                val index = config.deskPlotStatusTypes.indexOf(foundStatus)
                if (index < lowest) lowest = index
            }
        }
        return lowest
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        val list: MutableList<String> = mutableListOf()

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val plot = plots.find { it.inventorySlot == slot.slotIndex } ?: continue
            println(plot.id to GardenPlotAPI.getCurrentPlot()?.id)

            if (plot.pests >= 1) list.add("§cPests")
            if (plot.currentSpray != null) list.add("§eSprays")
            if (!plot.unlocked) list.add("§7Locked")
            if (plot == GardenPlotAPI.getCurrentPlot()) list.add("§aCurrent plot")

            println(list)

            val index = getLowestIndexItem(list)

            if (index != 999) {
                val status = config.deskPlotStatusTypes[index]
                handleCurrent(slot, plot, status)
            }
            list.clear()
        }
    }

    private fun isEnabled() =
        GardenAPI.inGarden() && InventoryUtils.openInventoryName() == "Configure Plots" && config.enabled
}
