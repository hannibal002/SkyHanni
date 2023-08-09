package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkyMartCopperPrice {
    private val pattern = "§c(?<amount>.*) Copper".toPattern()
    private var display = emptyList<List<Any>>()
    private val config get() = SkyHanniMod.feature.garden

    companion object {
        var inInventory = false
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "SkyMart") return

        inInventory = true
        val table = mutableMapOf<Pair<String, String>, Pair<Double, NEUInternalName>>()
        for (stack in event.inventoryItems.values) {
            for (line in stack.getLore()) {
                val internalName = stack.getInternalName()
                val lowestBin = internalName.getPriceOrNull() ?: continue

                pattern.matchMatcher(line) {
                    val amount = group("amount").replace(",", "").toInt()
                    val factor = lowestBin / amount
                    val perFormat = NumberUtil.format(factor)
                    val priceFormat = NumberUtil.format(lowestBin)
                    val amountFormat = NumberUtil.format(amount)

                    val name = stack.nameWithEnchantment!!
                    val advancedStats = if (config.skyMartCopperPriceAdvancedStats) {
                        " §7(§6$priceFormat §7/ §c$amountFormat Copper§7)"
                    } else ""
                    val pair = Pair("$name§f:", "§6§l$perFormat$advancedStats")
                    table[pair] = Pair(factor, internalName)
                }
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
                posLabel = "SkyMart Copper Price"
            )
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.skyMartCopperPrice
}
