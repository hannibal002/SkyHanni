package at.hannibal2.skyhanni.features.mining.mineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.mining.CorpseLootedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object MineshaftCorpseProfitPer {
    private val config get() = SkyHanniMod.feature.mining.mineshaft

    @SubscribeEvent
    fun onFossilExcavation(event: CorpseLootedEvent) {
        if (!config.profitPerCorpseLoot) return
        val loot = event.loot

        var totalProfit = 0.0
        val map = mutableMapOf<String, Double>()
        for ((name, amount) in loot) {
            if (name == "§bGlacite Powder") continue
            NEUInternalName.fromItemNameOrNull(name)?.let {
                val pricePer = it.getPrice()
                if (pricePer == -1.0) continue
                val profit = amount * pricePer
                val text = "§eFound $name §8${amount.addSeparators()}x §7(§6${profit.shortFormat()}§7)"
                map[text] = profit
                totalProfit += profit
            }
        }

        val corpseType = event.corpseType
        val name = corpseType.displayName

        corpseType.key?.let {
            val keyName = it.itemName
            val price = it.getPrice()

            map["$keyName: §c-${price.shortFormat()}"] = -price
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
