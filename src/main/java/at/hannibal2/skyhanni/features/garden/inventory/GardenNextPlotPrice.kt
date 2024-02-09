package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenNextPlotPrice {

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!GardenAPI.inGarden()) return
        if (!GardenAPI.config.plotPrice) return

        if (InventoryUtils.openInventoryName() != "Configure Plots") return

        val name = event.itemStack.name ?: return
        if (!name.startsWith("§ePlot")) return

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
                    val lowestBin = NEUItems.getPrice(NEUItems.getRawInternalName(itemName))
                    val price = lowestBin * amount
                    val format = NumberUtil.format(price)
                    list[i] = list[i] + " §7(§6$format§7)"
                } ?: {
                    ChatUtils.error("Could not read item '$line'")
                }
                break
            }
        }
    }
}
