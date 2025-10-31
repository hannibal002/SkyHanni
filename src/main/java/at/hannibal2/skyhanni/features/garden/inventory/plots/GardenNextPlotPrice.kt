package at.hannibal2.hanni.features.garden.inventory.plots

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.features.garden.GardenApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.hanni.utils.ItemUtils
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NumberUtil.shortFormat

@HanniModule
object GardenNextPlotPrice {

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onToolTip(event: ToolTipEvent) {
        if (!GardenApi.config.plotPrice) return

        if (InventoryUtils.openInventoryName() != "Configure Plots") return

        if (!event.itemStack.displayName.startsWith("§ePlot")) return

        var next = false
        val list = event.toolTip
        var i = -1
        for (line in event.toolTipRemovedPrefix()) {
            i++
            if (line.contains("Cost")) {
                next = true
                continue
            }

            if (next) {
                val readItemAmount = ItemUtils.readItemAmount(line)
                readItemAmount?.let {
                    val (itemName, amount) = it
                    val lowestBin = NeuInternalName.fromItemName(itemName).getPrice()
                    val price = lowestBin * amount
                    val format = price.shortFormat()
                    list[i] = list[i] + " §7(§6$format§7)"
                } ?: run {
                    ErrorManager.logErrorStateWithData(
                        "Garden Next Plot Price error",
                        "Could not read item amount from line",
                        "line" to line,
                        "event.toolTip" to event.toolTip,
                    )
                }
                break
            }
        }
    }
}
