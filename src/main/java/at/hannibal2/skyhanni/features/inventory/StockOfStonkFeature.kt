package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.transformAt
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component

@SkyHanniModule
object StockOfStonkFeature {

    private val config get() = SkyHanniMod.feature.inventory
    private val storage get() = ProfileStorageData.profileSpecific?.stonksAuction

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
     * WRAPPED-REGEX-TEST: "   Minimum Bid: 2,400,002 Coins"
     * WRAPPED-REGEX-TEST: "   Minimum Bid: 2,400,002 Coins"
     */
    private val bidPattern by patternGroup.pattern(
        "bid.new",
        " {3}Minimum Bid: (?<amount>[\\d,]+) Coins",
    )

    /**
     * REGEX-TEST: Your current bid: None
     * REGEX-TEST: Your current bid: 2,500,500 Coins
     */
    private val currentBidPattern by patternGroup.pattern(
        "currentbid",
        "Your current bid: (?:None|(?<amount>[\\d,]+) Coins)",
    )

    /**
     * REGEX-TEST: Auction ends in 4h 32m
     */
    private val auctionEndsPattern by patternGroup.pattern(
        "endsin",
        "Auction ends in (?<duration>(?:\\d+[dhms] ?)+)",
    )

    var inInventory = false

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryOpen(event: InventoryOpenEvent) {
        inInventory = inventoryPattern.matches(event.inventoryName)
    }

    @HandleEvent
    private fun onInventoryClose() {
        inInventory = false
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onToolTip(event: ToolTipTextEvent) {
        if (!inInventory) return
        if (!inventoryPattern.matches(event.itemStack.cleanName)) return

        updateAuctionData(event.toolTip)

        if (!config.stonkOfStonkPrice) return
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
        event.toolTip.transformAt(bestValueIndex) { replace("§6§6", "§a") ?: this }
    }

    private fun updateAuctionData(toolTip: List<Component>) {
        val storage = storage ?: return
        var roundEnd: SimpleTimeMark? = null
        var bidAmount: Long? = null
        for (line in toolTip) {
            auctionEndsPattern.matchMatcher(line) {
                val duration = TimeUtils.getDurationOrNull(group("duration")) ?: return@matchMatcher
                roundEnd = duration.fromNow()
            }
            currentBidPattern.matchMatcher(line) {
                bidAmount = groupOrNull("amount")?.formatLong()
            }
        }
        val newRoundEnd = roundEnd ?: return
        storage.roundEnd = newRoundEnd
        bidAmount?.let {
            storage.lastBidRoundEnd = newRoundEnd
            storage.bidAmount = it
        }
    }
}
