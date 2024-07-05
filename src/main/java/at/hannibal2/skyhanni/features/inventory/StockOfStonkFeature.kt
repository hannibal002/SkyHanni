package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.transformAt
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object StockOfStonkFeature {

    private val config get() = SkyHanniMod.feature.inventory.stonkOfStonkPrice

    private val inventoryPattern by RepoPattern.pattern("inventory.stockofstonk.inventory", "Stonks Auction")
    private val itemPattern by RepoPattern.pattern("inventory.stockofstonk.item", "§dStonks Auction")
    private val topPattern by RepoPattern.pattern(
        "inventory.stockofstonk.top",
        "§5§o§7§7▶ §c§lTOP (?<rank>[\\d,]+)§7 - §5Stock of Stonks §8x(?<amount>\\d+)",
    )
    private val bidPattern by RepoPattern.pattern("inventory.stockofstonk.bid", "§5§o§7   Minimum Bid: §6(?<amount>[\\d,]+) Coins")

    @SubscribeEvent
    fun onLorenzToolTip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        if (!itemPattern.matches(event.itemStack.displayName)) return
        var stonksReward = 0
        var index = 0
        var bestValueIndex = 0
        var bestRatio = Long.MAX_VALUE
        loop@ while (index < event.toolTip.size) {
            val line = event.toolTip[index]
            index++
            topPattern.matchMatcher(line) {
                stonksReward = group("amount").toInt()
                continue@loop
            }
            bidPattern.matchMatcher(line) {
                val cost = group("amount").replace(",", "").toLong()
                val ratio = cost / stonksReward
                event.toolTip[index - 1] = line + " §7(§6§6${ratio.addSeparators()} §7per)" // double §6 for the replacement at the end
                if (ratio < bestRatio) {
                    bestValueIndex = index - 1
                    bestRatio = ratio
                }
            }
        }
        event.toolTip.transformAt(bestValueIndex) { replace("§6§6", "§a") }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config && inventoryPattern.matches(InventoryUtils.openInventoryName())
}
