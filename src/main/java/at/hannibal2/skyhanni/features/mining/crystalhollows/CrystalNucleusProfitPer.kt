package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGH
import at.hannibal2.skyhanni.events.mining.CrystalNucleusLootEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import tv.twitch.chat.Chat

@SkyHanniModule
object CrystalNucleusProfitPer {
    private val config get() = SkyHanniMod.feature.mining.crystalNucleusTracker

    val jungleKeyItem = "JUNGLE_KEY".asInternalName()
    val robotPartItems = listOf(
        "CONTROL_SWITCH",
        "ELECTRON_TRANSMITTER",
        "FTX_3070",
        "ROBOTRON_REFLECTOR",
        "SUPERLITE_MOTOR",
        "SYNTHETIC_HEART",
    ).map { it.asInternalName() }

    @HandleEvent(priority = HIGH)
    fun onCrystalNucleusLoot(event: CrystalNucleusLootEvent) {
        if (!config.profitPer) return
        val loot = event.loot
        ChatUtils.chat("Loot size: ${loot.size}")

        var totalProfit = 0.0
        val map = mutableMapOf<String, Double>()
        for ((internalName, amount) in loot) {
            // Gemstone and Mithril Powder
            if (internalName.itemName.contains(" Powder")) continue
            internalName.getPrice().takeIf { price -> price != -1.0 }?.let { pricePer ->
                ChatUtils.chat("Found ${internalName.itemName} x${amount.addSeparators()} (Profit: ${pricePer.shortFormat()})")
                val profit = amount * pricePer
                val text = "§eFound ${internalName.itemName} §8${amount.addSeparators()}x §7(§6${profit.shortFormat()}§7)"
                map.addOrPut(text, profit)
                totalProfit += profit
            } ?: {
                ChatUtils.chat("Found ${internalName.itemName} x${amount.addSeparators()} (Profit: Unknown)")
                val text = "§eFound ${internalName.itemName} §8${amount.addSeparators()}x §7(§cUnknown§7)"
                map.addOrPut(text, 0.0)
            }
        }

        val jungleKeyPrice = jungleKeyItem.getPrice()
        map["§cUsed §5Jungle Key§7: §c-${jungleKeyPrice.shortFormat()}"] = -jungleKeyPrice
        totalProfit -= jungleKeyPrice

        var robotPartsPrice = 0.0
        robotPartItems.forEach { robotPartsPrice += it.getPrice() }
        map["§cUsed §9Robot Parts§7: §c-${robotPartsPrice.shortFormat()}"] = -robotPartsPrice
        totalProfit -= robotPartsPrice

        ChatUtils.chat("map size: " + map.size)

        val hover = map.sortedDesc().filter {
            (it.value >= config.profitPerMinimum) || it.value < 0
        }.keys.toMutableList()

        ChatUtils.chat("hover size: " + hover.size)

        if (hover.size != map.size) hover.add("§7${map.size - hover.size} cheap items are hidden.")
        val profitPrefix =
            if (totalProfit < 0) "§c"
            else "§6"
        val totalMessage = "Profit for Crystal Nucleus Run§e: $profitPrefix${totalProfit.shortFormat()}"
        hover.add("")
        hover.add("§e$totalMessage")
        ChatUtils.hoverableChat(totalMessage, hover)
    }
}
