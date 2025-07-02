package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object CompactStarlynSisters {

    private val config get() = SkyHanniMod.feature.foraging.starlyn
    private val patternGroup = RepoPattern.group("foraging.agatha")

    // would rather keep the extra int-related capture groups, you never know when you'll need them

    /**
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §r§lCOMMON §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §a§lUNCOMMON §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §9§lRARE §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §9§lEPIC §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §9§lLEGENDARY §fBracket in my contest!

     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §rCOMMON §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §aUNCOMMON §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §9RARE §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §9EPIC §fBracket in my contest!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rYou reached the §9LEGENDARY §fBracket in my contest!
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
        "§e\\[NPC] (?<foragingSister>[\\S ]+)§f: §rYour previous Personal Best was (?<previousBest>§.(?<prevBestInt>[\\d,]+))(?:§.)?\\."
    )

    /***
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rCome see me at §2Murkwater Loch §fto claim your rewards!
     * REGEX-TEST: §e[NPC] §bPlaceholder Name§f: §rCome see me at §2Murkwater Loch §fto claim your rewards!
     */
    @Suppress("MaxLineLength")
    private val seeMePattern by patternGroup.pattern(
        "claim-rewards",
        "§e\\[NPC] (?<foragingSister>[\\S ]+)§f: §rCome see me at (?<location>(?:§.)?.+) (?:§.)?to claim your rewards!"
    )

    /**
     * REGEX-TEST: §6§lPERSONAL BEST§f: You increased your §bFig §fCollection by §b5,129 §fduring the contest! That's §a5,129 §fmore than your previous best!
     * REGEX-TEST: §6§lPERSONAL BEST§f: You increased your §bFig §fCollection by §b129 §fduring the contest! That's §a0 §fmore than your previous best!
     * REGEX-TEST: §6§lPERSONAL BEST§f: You increased your §bFig §fCollection by §b434,325,129 §fduring the contest! That's §a234,455,129 §fmore than your previous best!
     * REGEX-TEST: §6§lPERSONAL BEST§r§f: You increased your §r§bMangrove §r§fCollection by §r§b2,505 §r§fduring the contest! That's §r§a358 §r§fmore than your previous best!
     */
    @Suppress("MaxLineLength")
    private val duringContestPersonalBestPattern by patternGroup.pattern(
        "collection-personal-best",
        "(?:§.)*PERSONAL BEST(?:§.)*: You increased your (?<woodTypeDisplay>(?:§.)*(?<woodType>\\w+)) (?:§.)*Collection by (?<duringContestDisplay>(?:§.)*(?<duringContest>[\\d,]+)) (?:§.)*during the contest! That's (?<aLotMore>(?:§.)*(?<byHowMuch>[\\d,]+)) (?:§.)*more than your previous best!"
    )

    /**
     * REGEX-TEST: §6Your total §2∮ Sweep §6is now increased by §21.28%§6!
     * REGEX-TEST: §6Your total §2∮ Sweep §6is now increased by §210%§6!
     * REGEX-TEST: §r§6Your total §r§2∮ Sweep §r§6is now increased by §r§21.28%§r§6!
     * REGEX-TEST: §r§6Your total §r§2∮ Sweep §r§6is now increased by §r§210%§r§6!
     */
    @Suppress("MaxLineLength")
    private val sweepIncreasePattern by patternGroup.pattern(
        "sweep-from-collection-pb",
        "(?:§.)*Your total (?:§.)*. Sweep (?:§.)*is now increased by (?<sweepIncreaseDisplay>(?:§.)*(?<sweepIncreasePercent>\\d+(?:\\.\\d+)?)%)(?:§.)*!",
    )

    /**
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e5129 §fFig logs collected in my Contest§f!
     * REGEX-TEST: §r§e[NPC] §r§bAgatha§r§f: §r§6§lPERSONAL BEST§r§f! You've surpassed your previous record of §r§e5129 §r§fFig logs collected in my Contest§r§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e5,129 §fFig logs collected in my Contest§f!
     * REGEX-TEST: §r§e[NPC] §r§bAgatha§r§f: §r§6§lPERSONAL BEST§r§f! You've surpassed your previous record of §r§e5,129 §r§fFig logs collected in my Contest§r§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e1,235,129 §fFig logs collected in my Contest§f!
     * REGEX-TEST: §r§e[NPC] §r§bAgatha§r§f: §r§6§lPERSONAL BEST§r§f! You've surpassed your previous record of §r§e1,235,129 §r§fFig logs collected in my Contest§r§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e129 §fFig logs collected in my Contest§f!
     * REGEX-TEST: §r§e[NPC] §r§bAgatha§r§f: §r§6§lPERSONAL BEST§r§f! You've surpassed your previous record of §r§e129 §r§fFig logs collected in my Contest§r§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e2,678 §fMangrove logs collected in my Contest§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §6§lPERSONAL BEST§f! You've surpassed your previous record of §e22,989 §fFig logs collected in my Contest§r§f!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §r§6§lPERSONAL BEST§f! You've surpassed your previous record of §e2,696 §fMangrove logs collected in my Contest§f!
     */
    @Suppress("MaxLineLength")
    private val sisterCollPBDuringContestPattern by patternGroup.pattern(
        "coll-pb-during-contest",
        "(?:§.)*\\[NPC] (?<foragingSister>(?:§.)*[\\w ]+)(?:§.)*: (?:§.)*PERSONAL BEST(?:§.)*! You've surpassed your previous record of (?:§.)*§e(?<previousRecord>[\\d, ]+) (?:§.)*(?<woodType>\\w+) logs collected in my Contest(?:§.)*!",
    )

    /**
     * REGEX-TEST: §e[NPC] §bAgatha§f: §rKeep it up!
     * REGEX-TEST: §r§e[NPC] §r§bAgatha§r§f: §rKeep it up!
     * REGEX-TEST: §e[NPC] §bAgatha§f: §fKeep it up!
     * REGEX-TEST: §r§e[NPC] §r§bAgatha§r§f: §r§fKeep it up!
     * REGEX-TEST: §e[NPC] §bAgatha§f: Keep it up!
     * REGEX-TEST: §r§e[NPC] §r§bAgatha§r§f: Keep it up!
     */
    @Suppress("MaxLineLength")
    private val sisterKeepItUpPattern by patternGroup.pattern(
        "keep-it-up-during-contest",
        "(?:§.)*\\[NPC] (?<foragingSister>(?:§.)*[\\w ]+)(?:§.)*: (?:§.)*Keep it up!",
    )

    private var isInResults = false
    private var contestVariablesAreDirty = false

    private var lastBracket = ""
    private var lastBracketPrefix = ""
    private var lastContestStartTime: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastLocation = ""
    private var lastPersonalBest = -1
    private var lastPersonalBestDisplay = ""
    private var lastPersonalBestStatus = false
    private var lastScore = -1
    private var lastScoreDisplay = ""
    private var lastSister = ""

    private var isInPersonalBest = false
    private var personalBestVariablesAreDirty = false

    private var lastPBWoodType = ""
    private var lastPBWoodTypeDisplay = ""
    private var lastPBCollectionIncreaseDuringContest = -1
    private var lastPBCollectionIncreaseDuringContestDisplay = ""
    private var lastPBPreviousBestDifference = -1
    private var lastPBPreviousBestDifferenceDisplay = ""
    private var lastPBSweepIncreaseDisplay = ""
    private var lastPBSweepIncrease = -1.0

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
                "§b$foragingSister's §eContest: §fYou broke a §dpersonal best §fof " +
                    "§b$previousRecord §e$woodType logs §fcollected during a contest! §eKeep it up!"
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
                lastPBWoodTypeDisplay = group("woodTypeDisplay")
                lastPBWoodType = group("woodType")
                lastPBCollectionIncreaseDuringContestDisplay = group("duringContestDisplay")
                lastPBCollectionIncreaseDuringContest = group("duringContest").formatInt()
                lastPBPreviousBestDifferenceDisplay = group("aLotMore")
                lastPBPreviousBestDifference = group("byHowMuch").formatInt()
                blockedReason = "STARLYN_COLLECTION"
                return
            }
        } else {
            sweepIncreasePattern.matchMatcher(message) {
                lastPBSweepIncreaseDisplay = group("sweepIncreaseDisplay")
                lastPBSweepIncrease = group("sweepIncreasePercent").formatDouble()
                val formattedPersonalBest =
                    "§6$lastPBWoodType PB§e: Your §2Sweep §eincreased by $lastPBSweepIncreaseDisplay §efrom collecting " +
                        "$lastPBCollectionIncreaseDuringContestDisplay $lastPBWoodTypeDisplay §eLogs " +
                        "($lastPBPreviousBestDifferenceDisplay §emore than your previous record)!"
                val hoverablePersonalBest = formattedPersonalBest.asComponent()
                hoverablePersonalBest.hover = (
                    "§eClick to check your personal bests!\n§2Sweep Increase§7: $lastPBSweepIncreaseDisplay\n" +
                        "§6Collected§7: $lastPBCollectionIncreaseDuringContestDisplay $lastPBWoodTypeDisplay §eLogs\n" +
                        "§6PB Increase: $lastPBPreviousBestDifferenceDisplay $lastPBWoodTypeDisplay §eLogs"
                    ).asComponent()
                hoverablePersonalBest.onClick(onClick = {
                    ChatUtils.sendMessageToServer("/starlynsisterlevels")
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
                lastContestStartTime = SimpleTimeMark.now().minus(20.minutes)
                lastSister = group("foragingSister")
                lastBracketPrefix = group("formattingCode")
                lastBracket = group("bracket")
                blockedReason = "STARLYN_RESULTS"
            }
        } else {
            pointsEarnedPattern.matchMatcher(message) {
                lastScore = group("pointsInteger").formatInt()
                lastScoreDisplay = group("pointsString")
                // if group is null or empty, it was not a personal best. otherwise it was
                lastPersonalBestStatus = !this.groupOrNull("personalBest").isNullOrEmpty()
                blockedReason = "STARLYN_RESULTS"
            }
            if (lastPersonalBestStatus) {
                previousBestPattern.matchMatcher(message) {
                    lastPersonalBest = group("prevBestInt").formatInt()
                    lastPersonalBestDisplay = group("previousBest")
                    blockedReason = "STARLYN_RESULTS"
                }
            }
            seeMePattern.matchMatcher(message) {
                lastLocation = group("location")
                val formattedResults = if (!lastPersonalBestStatus || lastPersonalBest < 1)
                    "$lastSister's §eContest: You earned §r$lastScoreDisplay §epoints, " +
                        "placing you in the $lastBracketPrefix$lastBracket §ebracket!"
                else
                    "$lastSister's §eContest: You earned §r$lastScoreDisplay §epoints, " +
                        "placing you in the $lastBracketPrefix$lastBracket §ebracket! " +
                        "Your previous §dpersonal best §ewas $lastPersonalBestDisplay §epoints!"
                val hoverableResults = formattedResults.asComponent()
                hoverableResults.hover = (
                    "§eClick to claim your rewards!"
                    ).asComponent()
                hoverableResults.onClick(onClick = {
                    ChatUtils.sendMessageToServer("/starlynsisterlevels")
                })
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

        lastBracket = ""
        lastBracketPrefix = ""
        lastContestStartTime = SimpleTimeMark.farPast()
        lastLocation = ""
        lastPersonalBest = -1
        lastPersonalBestDisplay = ""
        lastPersonalBestStatus = false
        lastScore = -1
        lastScoreDisplay = ""
        lastSister = ""

        contestVariablesAreDirty = false
    }

    private fun resetPersonalBestVariables() {
        if (!personalBestVariablesAreDirty) return

        lastPBWoodType = ""
        lastPBWoodTypeDisplay = ""
        lastPBCollectionIncreaseDuringContest = -1
        lastPBCollectionIncreaseDuringContestDisplay = ""
        lastPBPreviousBestDifference = -1
        lastPBPreviousBestDifferenceDisplay = ""
        lastPBSweepIncreaseDisplay = ""
        lastPBSweepIncrease = -1.0

        personalBestVariablesAreDirty = false
    }

    private fun isInIsland() = IslandTypeTags.FORAGING_CUSTOM_TREES.inAny()
}