package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.contests.rewards.ContestRewardSet
import at.hannibal2.skyhanni.events.garden.contests.rewards.ContestRewardsClaimedEvent
import at.hannibal2.skyhanni.features.garden.AnitaMedalProfit
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object CompactJacobClaim {

    private val config get() = SkyHanniMod.feature.chat
    private val patternGroup = RepoPattern.group("chat.jacobcompact")

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST:   FARMING CONTEST REWARDS CLAIMED
     */
    private val openingPattern by patternGroup.pattern(
        "opening.colorless",
        " {2}FARMING CONTEST REWARDS CLAIMED",
    )

    /**
     * REGEX-TEST:    » Sunflower Contest on Autumn 31st, Year 472
     * REGEX-TEST:    » Melon Slice Contest on Early Spring 17th, Year 473
     */
    private val specificContestLine by patternGroup.pattern(
        "contest.specific",
        " {3}» (?<crop>[\\w ]+) Contest on (?<season>[\\w ]+) (?<day>\\d+[stndh]{2,3}), Year (?<year>\\d+)",
    )

    /**
     * REGEX-TEST:   REWARDS
     */
    private val rewardsHeaderPattern by patternGroup.pattern(
        "rewards.colorless",
        " {2}REWARDS",
    )

    /**
     * REGEX-TEST:     Jacob's Ticket x271
     * REGEX-TEST:     Carnival Ticket x21
     * REGEX-TEST:     Carnival Ticket
     */
    private val ticketPattern by patternGroup.pattern(
        "tickets.colorless",
        " {4}(?<type>Jacob's|Carnival) Ticket(?: x(?<amount>[\\d,]+))?",
    )

    /**
     * REGEX-TEST:     1x Turbo-Cacti I Book
     * REGEX-TEST:     1x Turbo-Pumpkin I Book
     * REGEX-TEST:     4x Turbo-Wheat I Book
     * REGEX-TEST:     6x Turbo-Mushrooms I Book
     * REGEX-TEST:     1x Turbo-Warts I Book
     * REGEX-TEST:     1x Turbo-Sunflower I Book
     */
    private val bookPattern by patternGroup.pattern(
        "book.colorless",
        " {4}(?<amount>[\\d,]+)x Turbo-(?<crop>[^ ]+) I Book",
    )

    /**
     * REGEX-TEST:     +8 gold medals
     * REGEX-TEST:     +7 silver medals
     * REGEX-TEST:     +5 bronze medals
     * REGEX-TEST:     +1 gold medal
     */
    private val medalsPattern by patternGroup.pattern(
        "medals.colorless",
        " {4}\\+(?<amount>[\\d,]+) (?<type>[^ ]+) medals?",
    )

    /**
     * REGEX-TEST:     +2,293 Bits
     */
    private val bitsPattern by patternGroup.pattern(
        "bits.colorless",
        " {4}\\+(?<amount>[\\d,]+) Bits",
    )

    /**
     * REGEX-TEST:     Overclocker 3000
     * REGEX-TEST:     Overclocker 3000 x4
     */
    private val overclockerPattern by patternGroup.pattern(
        "overclocker",
        " {4}Overclocker 3000(?: x(?<count>\\d+))?",
    )
    // </editor-fold>

    private val rewardSet = ContestRewardSet()
    private val messageSet: MutableList<String> = mutableListOf()
    private var inLoop = false

    private fun publishEvent() {
        ContestRewardsClaimedEvent(rewardSet, messageSet).post()
        rewardSet.reset()
        messageSet.clear()
    }

    private fun SkyHanniChatEvent.Allow.block(reason: String) {
        // We need the message with color for the hover
        @Suppress("DEPRECATION")
        messageSet.add(message)
        blockedReason = reason
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.compactJacobClaim) return
        val message = event.cleanMessage
        var eventDelay = 300.milliseconds

        openingPattern.matchMatcher(message) {
            inLoop = true
            return event.block("compact_jacob_bulk_claim")
        }

        if (!inLoop) return

        specificContestLine.matchMatcher(message) {
            return event.block("compact_jacob_bulk_claim")
        }

        rewardsHeaderPattern.matchMatcher(message) {
            return event.block("compact_jacob_bulk_claim")
        }

        // Store the hash before processing this message so we can see if rewards changed
        val startingHash = rewardSet.hashCode()

        ticketPattern.matchMatcher(message) {
            val amount = groupOrNull("amount")?.formatInt() ?: 1
            when (group("type")) {
                "Jacob's" -> rewardSet.jacobTickets += amount
                "Carnival" -> rewardSet.carnivalTickets += amount
            }
        }

        overclockerPattern.matchMatcher(message) {
            rewardSet.overclockers += groupOrNull("count")?.formatInt() ?: 1
        }

        bookPattern.matchMatcher(message) {
            val crop = CropType.getByNameOrNull(group("crop")) ?: when (group("crop").lowercase()) {
                "cacti" -> CropType.CACTUS
                "rose" -> CropType.WILD_ROSE
                else -> return@matchMatcher
            }
            val amount = group("amount").formatInt()
            rewardSet.books += (crop to amount)
        }

        medalsPattern.matchMatcher(message) {
            val amount = group("amount").formatInt()
            val type = group("type")
            val medalType = AnitaMedalProfit.MedalType.bySimpleNameOrNull(type) ?: return@matchMatcher
            rewardSet.medals += (medalType to amount)
        }

        bitsPattern.matchMatcher(message) {
            val amount = group("amount").formatInt()
            rewardSet.bits += amount
            // Bits are always the last reward, if we get them, we can assume no more rewards will come
            eventDelay = 0.milliseconds
        }

        // If the rewards changed, we need to block the 'offending' chat message
        if (rewardSet.hashCode() != startingHash) event.block("compact_jacob_bulk_claim")

        val hashNow = rewardSet.hashCode()
        DelayedRun.runDelayed(eventDelay) {
            if (rewardSet.hashCode() != hashNow) return@runDelayed
            inLoop = false
            publishEvent()
        }
    }

    @HandleEvent
    fun onContestRewardsClaimed(event: ContestRewardsClaimedEvent) {
        if (!config.compactJacobClaim) return
        event.sendCompact()
    }

    private fun ContestRewardsClaimedEvent.sendCompact() = ChatUtils.hoverableChat(
        listOf(
            "§6§lContest Rewards",
            getTicketsFormat(),
            getBooksFormat(),
            getMedalsFormat(),
            getBitsFormat(),
            getOverclockersFormat(),
        ).filterNot {
            it.isEmpty()
        }.joinToString(separator = " §8§l| §r"),
        hover = messages,
        prefix = false,
    )

    private fun ContestRewardsClaimedEvent.getTicketsFormat() = buildString {
        if (rewards.jacobTickets == 0 && rewards.carnivalTickets == 0) return@buildString
        val ticketList = listOf(
            "§a${rewards.jacobTickets} Ja".takeIf { rewards.jacobTickets > 0 }.orEmpty(),
            "§a${rewards.carnivalTickets} Ca".takeIf { rewards.carnivalTickets > 0 }.orEmpty(),
        ).filterNot { it.isEmpty() }
        append("Tickets: " + ticketList.joinToString(separator = "§7, ") { it })
    }

    private fun ContestRewardsClaimedEvent.getBooksFormat() = buildString {
        val books = rewards.books.filter { it.value > 0 }.takeIf { it.isNotEmpty() } ?: return@buildString
        val bookList = books.map { (crop, amount) ->
            "${crop.cropColor.getChatColor()}$amount ${crop.cropShortName}"
        }.sortedBy { it.removeColor() }
        append("Books: " + bookList.joinToString(separator = "§7, ") { it })
    }

    private fun ContestRewardsClaimedEvent.getMedalsFormat() = buildString {
        val medals = rewards.medals.filter { it.value > 0 }.takeIf { it.isNotEmpty() } ?: return@buildString
        val medalList = medals.toSortedMap(compareBy { it.ordinal }).map { (medalType, amount) ->
            "${medalType.color.getChatColor()}$amount"
        }
        append("Medals: " + medalList.joinToString(separator = "§7, ") { it })
    }

    private fun ContestRewardsClaimedEvent.getBitsFormat() = buildString {
        if (rewards.bits == 0) return@buildString
        append("Bits: §b${rewards.bits}")
    }

    private fun ContestRewardsClaimedEvent.getOverclockersFormat() = buildString {
        val ocCount = rewards.overclockers.takeIf { it > 0 } ?: return@buildString
        val ocFormat = StringUtils.pluralize(ocCount, "Overclocker")
        append("$ocFormat: §6$ocCount")
    }
}
