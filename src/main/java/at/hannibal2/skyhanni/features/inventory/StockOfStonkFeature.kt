package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.transformAt
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object StockOfStonkFeature {

    private val config get() = SkyHanniMod.feature.inventory

    private val patternGroup = RepoPattern.group("inventory.stockofstonks")

    /**
     * REGEX-TEST: Stonks Auction
     */
    private val inventoryPattern by patternGroup.pattern(
        "inventory",
        "Stonks Auction",
    )

    /**
     * REGEX-TEST: ▶ TOP 5,000 - Stock of Stonks x2
     * REGEX-TEST: ▶ TOP 5,000 - Stock of Stonks x2
     * REGEX-TEST: ▶ TOP 100 - Stock of Stonks x25
     */
    private val topPattern by patternGroup.pattern(
        "top.new",
        "▶ TOP (?<rank>[\\d,]+) - Stock of Stonks x(?<amount>\\d+)",
    )

    /**
     * REGEX-TEST:    Minimum Bid: 2,400,002 Coins
     * REGEX-TEST:    Minimum Bid: 2,400,002 Coins
     */
    private val bidPattern by patternGroup.pattern(
        "bid.new",
        " {3}Minimum Bid: (?<amount>[\\d,]+) Coins",
    )

    var inInventory = false

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (isEnabled()) {
            inInventory = inventoryPattern.matches(event.inventoryName)
        }
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        if (!inventoryPattern.matches(event.itemStack.cleanName())) return
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
                val cost = group("amount").formatLong().coerceAtLeast(2000000) // minimum bid is 2,000,000
                val ratio = cost / stonksReward.transformIf({ this == 0 }, { 1 })
                // double §6 for the replacement at the end
                event.toolTip[index - 1] = line.append(" §7(paying §6§6${ratio.addSeparators()} §7per)")
                if (ratio < bestRatio) {
                    bestValueIndex = index - 1
                    bestRatio = ratio
                }
            }
        }
        event.toolTip.transformAt(bestValueIndex) { replace("§6§6", "§a") }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.stonkOfStonkPrice
}
