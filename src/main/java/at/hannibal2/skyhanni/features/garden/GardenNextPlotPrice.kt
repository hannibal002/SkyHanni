package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.name
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenNextPlotPrice {

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.garden.plotPrice) return

        if (InventoryUtils.openInventoryName() != "Configure Plots") return

        val name = event.itemStack.name ?: return
        if (!name.startsWith("§ePlot")) return

        var next = false
        val list = event.toolTip
        var i = -1
        for (l in list) {
            i++
            val line = l.substring(4)
            if (line.contains("Cost")) {
                next = true
                continue
            }

            if (next) {
                val (itemName, amount) = ItemUtils.readItemAmount(line)
                if (itemName != null) {
                    val internalName = NEUItems.getInternalNameByName(itemName)
                    val auctionManager = NotEnoughUpdates.INSTANCE.manager.auctionManager
                    val lowestBin = auctionManager.getBazaarOrBin(internalName, false)
                    val price = lowestBin * amount
                    val format = NumberUtil.format(price)
                    list[i] = list[i] + " §f(§6$format§f)"
                }
                break
            }
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN
}