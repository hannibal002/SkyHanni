package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.million
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PurseApi {
    private val patternGroup = RepoPattern.group("data.purse")

    /**
     * REGEX-TEST: Piggy: §6423,085,766
     * REGEX-TEST: Purse: §6423,085,776 §e(+5)
     */
    val coinsPattern by patternGroup.pattern(
        "coins",
        "(?:§.)*(?:Piggy|Purse): §6(?<coins>[\\d,.]+)(?: ?(?:§.)*\\([+-](?<earned>[\\d,.]+)\\)?|.*)?$",
    )

    /**
     * REGEX-TEST: Piggy: §6423,085,766
     * REGEX-TEST: §6§fPiggy: §6301,788§j§6,444
     */
    val piggyPattern by patternGroup.pattern(
        "piggy",
        "(?:§.)*Piggy: (?<coins>.*)",
    )

    /**
     * REGEX-TEST: You collected 1,732,500 coins from selling [Lvl 18] Griffin to [VIP+] NottJeff in an auction!
     * REGEX-FAIL: [YOUTUBE] Akinsoft collected an auction for 1,732,500 coins!
     */
    private val auctionHouseSaleClaimPattern by patternGroup.pattern(
        "auctionhouse.saleclaim",
        "^You collected (?<coins>[\\d,]+) coins from selling .+ in an auction!$",
    )

    private data class PendingAuctionHouseGain(val coins: Double, val created: SimpleTimeMark)

    private var inventoryCloseTime = SimpleTimeMark.farPast()
    private val pendingAuctionHouseGains = mutableListOf<PendingAuctionHouseGain>()
    var currentPurse = 0.0
        private set

    @HandleEvent
    private fun onInventoryClose(event: InventoryCloseEvent) {
        inventoryCloseTime = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        auctionHouseSaleClaimPattern.matchMatcher(event.cleanMessage) {
            pendingAuctionHouseGains.add(PendingAuctionHouseGain(group("coins").formatDouble(), SimpleTimeMark.now()))
        }
    }

    @HandleEvent
    private fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        coinsPattern.firstMatcher(event.added) {
            val newPurse = group("coins").formatDouble()
            val diff = newPurse - currentPurse
            if (diff == 0.0) return
            currentPurse = newPurse

            PurseChangeEvent(diff, currentPurse, getCause(diff)).post()
        }
    }

    // TODO add more causes in the future (e.g. ah/bz/bank)
    private fun getCause(diff: Double): PurseChangeCause {
        if (diff > 0) {
            if (diff == 1.0) {
                return PurseChangeCause.GAIN_TALISMAN_OF_COINS
            }

            // TODO relic of coins support
            if (diff == 15.million || diff == 100.million) {
                return PurseChangeCause.GAIN_DICE_ROLL
            }

            pendingAuctionHouseGains.removeAll { it.created.passedSince() >= 5.seconds }
            val exactGain = pendingAuctionHouseGains.indexOfFirst { it.coins == diff }
            if (exactGain != -1) {
                pendingAuctionHouseGains.removeAt(exactGain)
                return PurseChangeCause.GAIN_AUCTION_HOUSE
            }
            if (pendingAuctionHouseGains.isNotEmpty() && pendingAuctionHouseGains.sumOf { it.coins } == diff) {
                pendingAuctionHouseGains.clear()
                return PurseChangeCause.GAIN_AUCTION_HOUSE
            }

            if (MinecraftCompat.screen == null) {
                if (inventoryCloseTime.passedSince() > 2.seconds) {
                    return PurseChangeCause.GAIN_MOB_KILL
                }
            }
            return PurseChangeCause.GAIN_UNKNOWN
        } else {
            if (SlayerApi.questStartTime.passedSince() < 1.5.seconds) {
                return PurseChangeCause.LOSE_SLAYER_QUEST_STARTED
            }

            if (diff == -6_666_666.0 || diff == -666_666.0) {
                return PurseChangeCause.LOSE_DICE_ROLL_COST
            }

            return PurseChangeCause.LOSE_UNKNOWN
        }
    }

    fun getPurse(): Double = currentPurse
}
