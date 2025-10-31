package at.hannibal2.hanni.features.mining.fossilexcavator

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.mining.FossilExcavationEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.hanni.utils.ItemUtils.repoItemName
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.collection.CollectionUtils.sortedDesc

@HanniModule
object ProfitPerExcavation {
    private val config get() = HanniMod.feature.mining.fossilExcavator

    @HandleEvent
    fun onFossilExcavation(event: FossilExcavationEvent) {
        if (!config.profitPerExcavation) return
        val loot = event.loot

        var totalProfit = 0.0
        val map = mutableMapOf<String, Double>()
        for ((name, amount) in loot) {
            if (name == "§bGlacite Powder") continue
            NeuInternalName.fromItemNameOrNull(name)?.let {
                val pricePer = it.getPrice()
                if (pricePer == -1.0) continue
                val profit = amount * pricePer
                val text = "§eFound $name §8${amount.addSeparators()}x §7(§6${profit.shortFormat()}§7)"
                map[text] = profit
                totalProfit += profit
            }
        }

        val scrapItem = FossilExcavatorApi.scrapItem

        val scrapPrice = scrapItem.getPrice()
        map["${scrapItem.repoItemName}: §c-${scrapPrice.shortFormat()}"] = -scrapPrice
        totalProfit -= scrapPrice

        val hover = map.sortedDesc().keys.toMutableList()
        val profitPrefix = if (totalProfit < 0) "§c" else "§6"
        val totalMessage = "Profit this excavation: $profitPrefix${totalProfit.shortFormat()}"
        hover.add("")
        hover.add("§e$totalMessage")
        ChatUtils.hoverableChat(totalMessage, hover)
    }
}
