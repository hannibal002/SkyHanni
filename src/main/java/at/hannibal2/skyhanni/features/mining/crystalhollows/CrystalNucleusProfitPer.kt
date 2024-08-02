package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.HIGH
import at.hannibal2.skyhanni.events.mining.CrystalNucleusLootEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat

@SkyHanniModule
object CrystalNucleusProfitPer {
    private val config get() = SkyHanniMod.feature.mining.crystalNucleusTracker

    private val robotParts = listOf(
        "CONTROL_SWITCH",
        "ELECTRON_TRANSMITTER",
        "FTX_3070",
        "ROBOTRON_REFLECTOR",
        "SUPERLITE_MOTOR",
        "SYNTHETIC_HEART",
    )

    @HandleEvent(priority = HIGH)
    fun onCrystalNucleusLoot(event: CrystalNucleusLootEvent) {
        if (!config.profitPer) return
        val loot = event.loot

        var totalProfit = 0.0
        val map = mutableMapOf<String, Double>()
        for ((name, amount) in loot) {
            // Gemstone and Mithril Powder
            if (name.contains(" Powder")) continue
            NEUInternalName.fromItemNameOrNull(name)?.let {
                val pricePer = it.getPrice()
                if (pricePer == -1.0) continue
                val profit = amount * pricePer
                val text = "§eFound $name §8${amount.addSeparators()}x §7(§6${profit.shortFormat()}§7)"
                map[text] = profit
                totalProfit += profit
            }
        }

        val jungleKeyPrice = "JUNGLE_KEY".asInternalName().getPrice()
        map["§9Used §5Jungle Key§7: §c-${jungleKeyPrice.shortFormat()}"] = -jungleKeyPrice
        totalProfit -= jungleKeyPrice

        var robotPartsPrice = 0.0
        robotParts.forEach { robotPartsPrice += it.asInternalName().getPrice() }
        map["§9Used §9Robot Parts§7: §c-${robotPartsPrice.shortFormat()}"] = -robotPartsPrice
        totalProfit -= robotPartsPrice

        val hover = map.sortedDesc().filter { it.value >= config.profitPerMinimum || it.value < 0 }.keys.toMutableList()
        if (hover.size != map.size) hover.add("§7${map.size - hover.size} cheap items are hidden.")
        val profitPrefix = if (totalProfit < 0) "§c" else "§6"
        val totalMessage = "Profit for Crystal Nucleus Run§e: $profitPrefix${totalProfit.shortFormat()}"
        hover.add("")
        hover.add("§e$totalMessage")
        ChatUtils.hoverableChat(totalMessage, hover)
    }
}
