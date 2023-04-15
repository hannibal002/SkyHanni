package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class SkyMartCopperPrice {
    private val pattern = Pattern.compile("§c(.*) Copper")
    private var display = listOf<List<Any>>()
    private val config get() = SkyHanniMod.feature.garden

    companion object {
        var inInventory = false
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "SkyMart") return

        inInventory = true
        val table = mutableMapOf<Pair<String, String>, Pair<Double, String>>()
        for (stack in event.inventoryItems.values) {
            for (line in stack.getLore()) {
                val matcher = pattern.matcher(line)
                if (!matcher.matches()) continue

                val internalName = stack.getInternalName()
                val lowestBin = NEUItems.getPrice(internalName)
                if (lowestBin == -1.0) continue

                val amount = matcher.group(1).replace(",", "").toInt()
                val factor = lowestBin / amount
                val perFormat = NumberUtil.format(factor)
                val priceFormat = NumberUtil.format(lowestBin)
                val amountFormat = NumberUtil.format(amount)

                var name = stack.name!!
                if (name == "§fEnchanted Book") {
                    name = stack.getLore()[0]
                }

                val advancedStats = if (config.skyMartCopperPriceAdvancedStats) {
                    " §7(§6$priceFormat §7/ §c$amountFormat Copper§7)"
                } else ""
                val pair = Pair("$name§f:", "§6§l$perFormat$advancedStats")
                table[pair] = Pair(factor, internalName)
            }
        }

        val newList = mutableListOf<List<Any>>()
        newList.addAsSingletonList("§eCoins per Copper§f:")
        LorenzUtils.fillTable(newList, table)
        display = newList
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (inInventory) {
            config.skyMartCopperPricePos.renderStringsAndItems(
                display,
                extraSpace = 5,
                itemScale = 1.7,
                posLabel = "Sky Mart Copper Price"
            )
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.skyMartCopperPrice
}