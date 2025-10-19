package at.hannibal2.skyhanni.features.garden.inventory.plots

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.currentSpray
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.getLowestIndexStatus
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.getPlotStatuses
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.pests
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight

@SkyHanniModule
object GardenPlotMenuHighlighting {

    private val config get() = GardenApi.config.plotMenuHighlighting

    private val highlightedPlots = mutableMapOf<GardenPlotApi.Plot, GardenPlotApi.PlotStatusType>()

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val plot = GardenPlotApi.plots.find { it.inventorySlot == slot.slotIndex } ?: continue

            plot.getPlotStatuses().getLowestIndexStatus(config.deskPlotStatusTypes)?.let { status ->
                println(status)
                handleCurrent(plot, status)
            } ?: highlightedPlots.remove(plot)
        }
    }

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled() || highlightedPlots.isEmpty()) return

        for (plot in highlightedPlots) {
            val items = InventoryUtils.getItemsInOpenChest()
            if (plot.key.inventorySlot in items.indices) {
                val slot = items[plot.key.inventorySlot]
                slot.stack.stackSize = handleStackSize(plot.key, plot.value)
                slot.highlight(plot.value.highlightColor)
            }
        }
    }

    private fun handleStackSize(plot: GardenPlotApi.Plot, status: GardenPlotApi.PlotStatusType): Int {
        return when (status.name) {
            "§cPests" -> return plot.pests
            "§eSprays" -> return plot.currentSpray?.expiry?.timeUntil()?.inWholeMinutes?.toInt() ?: 1
            else -> 1
        }
    }

    private fun handleCurrent(plot: GardenPlotApi.Plot, status: GardenPlotApi.PlotStatusType) {
        val isHighlighted = highlightedPlots.containsKey(plot)
        val isCurrent = highlightedPlots[plot] == status
        if (!isHighlighted || isCurrent) {
            if (!isHighlighted) highlightedPlots[plot] = status
        } else {
            highlightedPlots[plot] = status
        }
    }

    private fun isEnabled() =
        GardenApi.inGarden() && InventoryUtils.openInventoryName() == "Configure Plots" && config.enabled
}
