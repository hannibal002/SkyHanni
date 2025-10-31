package at.hannibal2.hanni.features.mining.crystalhollows

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.api.event.HandleEvent.Companion.HIGH
import at.hannibal2.hanni.events.mining.CrystalNucleusLootEvent
import at.hannibal2.hanni.features.mining.crystalhollows.CrystalNucleusApi.JUNGLE_KEY_ITEM
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.hanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.hanni.utils.collection.CollectionUtils.sortedDesc

@HanniModule
object CrystalNucleusProfitPer {
    private val config get() = HanniMod.feature.mining.crystalNucleusTracker

    @HandleEvent(priority = HIGH)
    fun onCrystalNucleusLoot(event: CrystalNucleusLootEvent) {
        if (!config.profitPer) return
        val loot = event.loot

        var totalProfit = 0.0
        val map = mutableMapOf<String, Double>()
        for ((internalName, amount) in loot) {
            internalName.getPrice().takeIf { price: Double -> price != -1.0 }?.let { pricePer: Double ->
                val profit: Double = amount * pricePer
                val text = "§eFound ${internalName.getPriceName(amount, pricePer)}"
                map.addOrPut(text, profit)
                totalProfit += profit
            }
        }

        val hover = map.sortedDesc().filter {
            (it.value >= config.profitPerMinimum)
        }.keys.toMutableList()

        // Account for excluded items
        map.filter { it.key !in hover }.takeIf { it.isNotEmpty() }?.let {
            hover.add("§7${it.size} cheap items are hidden §7(§6${it.values.sum().shortFormat()}§7).")
        }

        val jungleKeyCost = JUNGLE_KEY_ITEM.getPrice()
        val partsCost = CrystalNucleusApi.getPrecursorRunPrice { it.getPrice() }
        totalProfit -= (jungleKeyCost + partsCost)

        val profitPrefix = if (totalProfit < 0) "§c" else "§6"
        val totalMessage = "Profit for Crystal Nucleus Run§e: $profitPrefix${totalProfit.shortFormat()}"

        hover.add("")
        hover.add("§cUsed §5Jungle Key§7: §c-${jungleKeyCost.shortFormat()}")
        hover.add("§cUsed §9Robot Parts§7: §c-${partsCost.shortFormat()}")
        hover.add("")
        hover.add("§e$totalMessage")

        ChatUtils.hoverableChat(totalMessage, hover)
    }
}
