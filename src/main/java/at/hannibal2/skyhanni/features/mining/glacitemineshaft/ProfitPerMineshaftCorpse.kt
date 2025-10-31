package at.hannibal2.hanni.features.mining.glacitemineshaft

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.mining.CorpseLootedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.hanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.hanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.hanni.utils.ItemUtils.repoItemName
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.collection.CollectionUtils.sortedDesc

@HanniModule
object ProfitPerMineshaftCorpse {
    private val config get() = HanniMod.feature.mining.mineshaft

    @HandleEvent
    fun onCorpseLooted(event: CorpseLootedEvent) {
        if (!config.profitPerCorpseLoot) return
        val loot = event.loot

        var totalProfit = 0.0
        val map = mutableMapOf<String, Double>()
        for ((name, amount) in loot) {
            if (name == "§bGlacite Powder") continue
            val internalName = NeuInternalName.fromItemNameOrNull(name) ?: continue
            val pricePer = internalName.getPriceOrNull() ?: continue
            val profit = amount * pricePer
            val text = "§eFound ${internalName.getPriceName(amount, pricePer)}"
            map[text] = profit
            totalProfit += profit
        }

        val corpseType = event.corpseType
        val name = corpseType.displayName

        corpseType.key?.let {
            val keyName = it.repoItemName
            val price = it.getPrice()

            map["§cCost: $keyName §7(§c-${price.shortFormat()}§7)"] = -price
            totalProfit -= price
        }

        val hover = map.sortedDesc().keys.toMutableList()
        val profitPrefix = if (totalProfit < 0) "§c" else "§6"
        val totalMessage = "Profit for $name Corpse§e: $profitPrefix${totalProfit.shortFormat()}"
        hover.add("")
        hover.add("§e$totalMessage")
        ChatUtils.hoverableChat(totalMessage, hover)
    }
}
