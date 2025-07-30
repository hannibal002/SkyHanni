package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CompactStarlynSisters {

    private val config get() = SkyHanniMod.feature.foraging.starlynContest
    private val patternGroup = RepoPattern.group("foraging.agatha")

    /**
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §r§lCOMMON §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §a§lUNCOMMON §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §9§lRARE §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §9§lEPIC §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §9§lLEGENDARY §fBracket in my contest!
     */
    private val startContestResultsPattern by patternGroup.pattern(
        "start-results",
        "§e\\[NPC] (?<foragingSister>[\\S ]+)§f: §rYou reached the (?<formattingCode>§.)(?:§.)?(?<bracket>\\w+) §fBracket in my contest!"
    )

    /**
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou earned a total of §b230 §fpoints!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou earned a total of §b2,506 §fpoints!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou earned a total of §b700 §fpoints! That's a new §d§lPERSONAL BEST§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou earned a total of §b123,700 §fpoints! That's a new §d§lPERSONAL BEST§f!
     */
    @Suppress("MaxLineLength")
    private val pointsEarnedPattern by patternGroup.pattern(
        "points-earned",
        "§e\\[NPC] (?<foragingSister>[\\S ]+)§f: §rYou earned a total of (?<pointsString>§.(?<pointsInteger>[\\d,]+)) §fpoints!(?<personalBest> That's a new (?:§.)*PERSONAL BEST(?:§.)?!)?"
    )

    /**
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYour previous Personal Best was §b687§f.
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYour previous Personal Best was §b6,487§f.
     * REGEX-TEST: §e[NPC] §bPlaceholder Name§f: §rYour previous Personal Best was §b6,487§f.
     */
    @Suppress("MaxLineLength")
    private val previousBestPattern by patternGroup.pattern(
        "previous-best",
        "(?:§.)*\\[NPC] (?<foragingSister>[\\S ]+)(?:§.)*: (?:§.)*Your previous Personal Best was (?<previousBest>(?:§.)*(?<prevBestInt>[\\d,]+))(?:§.)*\\."
    )

    /***
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rCome see me at §2Murkwater Loch §fto claim your rewards!
     * REGEX-TEST: §e[NPC] §bPlaceholder Name§f: §rCome see me at §2Murkwater Loch §fto claim your rewards!
     */
    @Suppress("MaxLineLength")
    private val seeMePattern by patternGroup.pattern(
        "claim-rewards",
        "(?:§.)*\\[NPC] (?<foragingSister>[\\S ]+)(?:§.)*: (?:§.)*Come see me at (?<location>(?:§.)*.+) (?:§.)*to claim your rewards!"
    )

    /**
     * REGEX-TEST: §6§lPERSONAL BEST§f: You increased your §bFig §fCollection by §b5,129 §fduring the contest! That's §a5,129 §fmore than your previous best!
     * REGEX-TEST: §6§lPERSONAL BEST§f: You increased your §bFig §fCollection by §b129 §fduring the contest! That's §a0 §fmore than your previous best!
     * REGEX-TEST: §6§lPERSONAL BEST§f: You increased your §bFig §fCollection by §b434,325,129 §fduring the contest! That's §a234,455,129 §fmore than your previous best!
     */
    @Suppress("MaxLineLength")
    private val duringContestPersonalBestPattern by patternGroup.pattern(
        "collection-personal-best",
        "(?:§.)*PERSONAL BEST(?:§.)*: You increased your (?<woodTypeDisplay>(?:§.)*(?<woodType>\\w+)) (?:§.)*Collection by (?<duringContestDisplay>(?:§.)*(?<duringContest>[\\d,]+)) (?:§.)*during the contest! That's (?<aLotMore>(?:§.)*(?<byHowMuch>[\\d,]+)) (?:§.)*more than your previous best!"
    )

    /**
     * REGEX-TEST: §6Your total §2∮ Sweep §6is now increased by §21.28%§6!
     * REGEX-TEST: §6Your total §2∮ Sweep §6is now increased by §210%§6!
     */
    @Suppress("MaxLineLength")
    private val sweepIncreasePattern by patternGroup.pattern(
        "sweep-from-collection-pb",
        "(?:§.)*Your total (?:§.)*. Sweep (?:§.)*is now increased by (?<sweepIncreaseDisplay>(?:§.)*(?<sweepIncreasePercent>[\\d.]+)%)(?:§.)*!",
    )

    /**
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e5129 §fFig logs collected in my Contest§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e5,129 §fFig logs collected in my Contest§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e1,235,129 §fFig logs collected in my Contest§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e129 §fFig logs collected in my Contest§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e2,678 §fMangrove logs collected in my Contest§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e22,989 §fFig logs collected in my Contest§f!
     */
    @Suppress("MaxLineLength")
    private val sisterCollPBDuringContestPattern by patternGroup.pattern(
        "coll-pb-during-contest",
        "(?:§.)*\\[NPC] (?<foragingSister>(?:§.)*[\\w ]+)(?:§.)*: (?:§.)*PERSONAL BEST(?:§.)*! You've surpassed your previous record of (?:§.)*§e(?<previousRecord>[\\d,]+) (?:§.)*(?<woodType>[\\S ]+) logs collected in my Contest(?:§.)*!",
    )

    /**
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rKeep it up!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §fKeep it up!
     * REGEX-TEST: §e[NPC] §bAgatha§f: Keep it up!
     */
    @Suppress("MaxLineLength")
    private val sisterKeepItUpPattern by patternGroup.pattern(
        "keep-it-up-during-contest",
        "(?:§.)*\\[NPC] (?<foragingSister>(?:§.)*[\\w ]+)(?:§.)*: (?:§.)*Keep it up!",
    )

    data class StarlynContestResults(
        var lastBracket: String = "",
        var lastBracketPrefix: String = "",
        var lastPBScore: Int = -1,
        var lastPersonalBestDisplay: String = "",
        var hadPreviousPB: Boolean = false,
        var lastScoreDisplay: String = "",
        var lastSister: String = "",
    ) : Resettable()

    data class StarlynCollectionPersonalBests(
        var lastPBWoodType: String = "",
        var lastPBWoodTypeDisplay: String = "",
        var lastPBCollectionIncreaseDuringContestDisplay: String = "",
        var lastPBPreviousBestDifferenceDisplay: String = "",
        var lastPBSweepIncreaseDisplay: String = "",
    ) : Resettable()

    private var isInResults = false
    private var contestVariablesAreDirty = false
    private var contestResult = StarlynContestResults()

    private var isInPersonalBest = false
    private var personalBestVariablesAreDirty = false
    private var collectionPB = StarlynCollectionPersonalBests()

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isInIsland()) return
        event.blockAndCompact()
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.oldIsland != IslandType.GALATEA) return
        resetContestResultVariables()
        resetPersonalBestVariables()
    }

    private fun SkyHanniChatEvent.blockAndCompact() {
        val message = this.message
        if (config.compactPersonalBest)
            compactCollectionPB(message)
        if (config.compactResults)
            compactContestResults(message)
    }

    private fun SkyHanniChatEvent.compactCollectionPB(message: String) {
        sisterCollPBDuringContestPattern.matchMatcher(message) {
            val foragingSister = group("foragingSister")
            val previousRecord = group("previousRecord")
            val woodType = group("woodType")
            val formattedLockInWarning = (
                "§b$foragingSister's §eContest: You broke a §dpersonal best §eof " +
                    "§b$previousRecord §6$woodType logs §ecollected during a contest! Keep it up!"
                )
            val hoverableLockInWarning = formattedLockInWarning.asComponent()
            ChatUtils.chat(hoverableLockInWarning)
            blockedReason = "STARLYN_COLLECTION"
            return
        }
        sisterKeepItUpPattern.matchMatcher(message) {
            blockedReason = "STARLYN_COLLECTION"
            return
        }
        if (!isInPersonalBest) {
            duringContestPersonalBestPattern.matchMatcher(message) {
                isInPersonalBest = true
                personalBestVariablesAreDirty = true
                collectionPB = StarlynCollectionPersonalBests()
                collectionPB.lastPBWoodTypeDisplay = group("woodTypeDisplay")
                collectionPB.lastPBWoodType = group("woodType")
                collectionPB.lastPBCollectionIncreaseDuringContestDisplay = group("duringContestDisplay")
                collectionPB.lastPBPreviousBestDifferenceDisplay = group("aLotMore")
                blockedReason = "STARLYN_COLLECTION"
                return
            }
        } else {
            sweepIncreasePattern.matchMatcher(message) {
                collectionPB.lastPBSweepIncreaseDisplay = group("sweepIncreaseDisplay")
                val formattedPersonalBest =
                    "§6${collectionPB.lastPBWoodType} PB§e: Your §2Sweep §eincreased " +
                        "by ${collectionPB.lastPBSweepIncreaseDisplay} §efrom collecting " +
                        "${collectionPB.lastPBCollectionIncreaseDuringContestDisplay} " +
                        "${collectionPB.lastPBWoodTypeDisplay} §bLogs " +
                        "§e(${collectionPB.lastPBPreviousBestDifferenceDisplay} " +
                        "§emore than your previous record)!"
                val hoverablePersonalBest = formattedPersonalBest.asComponent()
                hoverablePersonalBest.hover = (
                    "§eClick to check your personal bests!\n§2Sweep Increase§7: " +
                        "${collectionPB.lastPBSweepIncreaseDisplay}\n" +
                        "§6Collected§7: ${collectionPB.lastPBCollectionIncreaseDuringContestDisplay} " +
                        "${collectionPB.lastPBWoodTypeDisplay} §eLogs\n" +
                        "§6PB Increase: ${collectionPB.lastPBPreviousBestDifferenceDisplay} " +
                        "${collectionPB.lastPBWoodTypeDisplay} §eLogs"
                    ).asComponent()
                hoverablePersonalBest.onClick(onClick = {
                    HypixelCommands.starlynSisters()
                })
                ChatUtils.chat(hoverablePersonalBest)
                isInPersonalBest = false
                blockedReason = "STARLYN_COLLECTION"
                resetPersonalBestVariables()
            }
        }
    }

    private fun SkyHanniChatEvent.compactContestResults(message: String) {
        if (!isInResults) {
            startContestResultsPattern.matchMatcher(message) {
                isInResults = true
                contestVariablesAreDirty = true
                contestResult = StarlynContestResults()
                contestResult.lastSister = group("foragingSister")
                contestResult.lastBracketPrefix = group("formattingCode")
                contestResult.lastBracket = group("bracket")
                blockedReason = "STARLYN_RESULTS"
            }
        } else {
            pointsEarnedPattern.matchMatcher(message) {
                contestResult.lastScoreDisplay = group("pointsString")
                // if group is null or empty, it was not a personal best. otherwise it was
                contestResult.hadPreviousPB = !this.groupOrNull("personalBest").isNullOrEmpty()
                blockedReason = "STARLYN_RESULTS"
            }
            if (contestResult.hadPreviousPB) {
                previousBestPattern.matchMatcher(message) {
                    contestResult.lastPBScore = group("prevBestInt").formatInt()
                    contestResult.lastPersonalBestDisplay = group("previousBest")
                    blockedReason = "STARLYN_RESULTS"
                }
            }
            seeMePattern.matchMatcher(message) {
                var formattedResults =
                    "${contestResult.lastSister}'s §eContest: " +
                        "You earned §r${contestResult.lastScoreDisplay} §epoints, " +
                        "placing you in the " +
                        "${contestResult.lastBracketPrefix}${contestResult.lastBracket} " +
                        "§ebracket!"
                if (contestResult.hadPreviousPB && contestResult.lastPBScore > 1)
                    formattedResults += "Your previous §dpersonal best §ewas ${contestResult.lastPersonalBestDisplay} §epoints!"
                val hoverableResults = formattedResults.asComponent()
                hoverableResults.hover = (
                    "§eClick to claim your rewards!"
                    ).asComponent()
                hoverableResults.onClick(
                    onClick = {
                        HypixelCommands.starlynSisters()
                    },
                )
                ChatUtils.chat(hoverableResults)
                isInResults = false
                blockedReason = "STARLYN_RESULTS"
                resetContestResultVariables()
                return
            }
        }
    }

    private fun resetContestResultVariables() {
        if (!contestVariablesAreDirty) return

        contestResult.reset()

        contestVariablesAreDirty = false
    }

    private fun resetPersonalBestVariables() {
        if (!personalBestVariablesAreDirty) return

        collectionPB.reset()

        personalBestVariablesAreDirty = false
    }

    private fun isInIsland() = IslandTypeTags.FORAGING_CUSTOM_TREES.inAny()
}
