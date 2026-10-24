package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.mining.CorpseLootedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ProfitPerMineshaftCorpse {
    private val config get() = SkyHanniMod.feature.mining.mineshaft

    /**
     * REGEX-TEST: Glacite Powder
     */
    private val skipCorpseRewardPattern by RepoPattern.pattern(
        "mining.mineshaft.skip-corpse-reward",
        "Glacite Powder"
    )

    @HandleEvent
    private fun onCorpseLooted(event: CorpseLootedEvent) {
        if (!config.profitPerCorpseLoot) return
        val loot = event.loot

        var totalProfit = 0.0
        val map = mutableMapOf<String, Double>()
        for ((name, amount) in loot) {
            if (skipCorpseRewardPattern.matches(name)) continue
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
