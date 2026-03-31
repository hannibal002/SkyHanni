package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.PlayerSpecificStorage.BingoSession
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoData
import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoRanksJson
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.bingo.card.goals.BingoGoal
import at.hannibal2.skyhanni.features.bingo.card.goals.GoalType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.days

@SkyHanniModule
object BingoApi {

    // TODO replace with dynamic detection once we have secret bingo detection (maybe via repo?)
    private val BINGO_EVENT_DURATION = 7.days
    private val BINGO_NPC_OFFSET = 3.days

    private var ranks = mapOf<String, Int>()
    private var data: Map<String, BingoData> = emptyMap()

    private var bingoNpcHidden = false
    private var alixerHidden = false

    val bingoGoals get() = bingoStorage.goals
    val personalGoals get() = bingoGoals.values.filter { it.type == GoalType.PERSONAL }
    val communityGoals get() = bingoGoals.values.filter { it.type == GoalType.COMMUNITY }
    var lastBingoCardOpenTime = SimpleTimeMark.farPast()

    /**
     * REGEX-TEST:  §9Ⓑ §9Bingo
     */
    private val detectionPattern by RepoPattern.pattern(
        "bingo.detection.scoreboard",
        " §.Ⓑ §.Bingo",
    )

    private val titleDetectionPattern by RepoPattern.pattern(
        "bingo.detection.scoreboardtitle",
        "SKYBLOCK Ⓑ",
    )

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Bingo Card")

        if (!SkyBlockUtils.isBingoProfile) {
            event.addIrrelevant("not on bingo")
            return
        }

        event.addData {
            add("bingoGoals: ${bingoGoals.size}")
            for (bingoGoal in bingoGoals) {
                val goal = bingoGoal.value
                add("  type: '${goal.type}'")
                add("  displayName: '${goal.displayName}'")
                add("  description: '${goal.description}'")
                add("  guide: '${goal.guide}'")
                add("  done: '${goal.done}'")
                add("  highlight: '${goal.highlight}'")
                add("  communityGoalPercentage: '${goal.communtyGoalPercentage}'")
                val hiddenGoalData = goal.hiddenGoalData
                add("  hiddenGoalData")
                add("    unknownTip: '${hiddenGoalData.unknownTip}'")
                add("    nextHintTime: '${hiddenGoalData.nextHintTime}'")
                add("    tipNote: '${hiddenGoalData.tipNote}'")
                add(" ")

            }
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        ranks = event.getConstant<BingoRanksJson>("BingoRanks").ranks
        data = event.getConstant<BingoJson>("Bingo").bingoTips
    }

    fun getRankFromScoreboard(text: String): Int? {
        return if (detectionPattern.matches(text)) getRank(text)
        else if (titleDetectionPattern.matches(HypixelData.getScoreboardTitle()?.removeColor())) {
            getRank(HypixelData.getScoreboardTitle().orEmpty())
        } else null
    }

    fun getIconFromScoreboard(text: String) = getRankFromScoreboard(text)?.let { getIcon(it) }

    fun getRank(text: String) = ranks.entries.find { text.contains(it.key) }?.value

    fun getIcon(searchRank: Int) = ranks.entries.find { it.value == searchRank }?.key

    // We added the suffix (Community Goal) so that older skyhanni versions don't crash with the new repo data.
    fun getData(itemName: String) =
        data.filter { itemName.startsWith(it.key.split(" (Community Goal)")[0]) }.values.firstOrNull()

    fun BingoGoal.getData(): BingoData? = if (type == GoalType.COMMUNITY) {
        getData(displayName)
    } else {
        data[displayName]
    }

    val bingoStorage: BingoSession by lazy {
        val playerSpecific = ProfileStorageData.playerSpecific ?: error("playerSpecific is null")
        // we currently use seconds as key
        // TODO change the seconds entry to SimpleTimeMark, use a config migration
        val seconds = getStartOfMonth().toMillis() / 1000
        playerSpecific.bingoSessions.getOrPut(seconds) { BingoSession() }
    }

    private fun getStartOfMonth() = OffsetDateTime.of(
        TimeUtils.getCurrentLocalDate().plusDays(5).withDayOfMonth(1),
        LocalTime.MIDNIGHT, ZoneOffset.UTC,
    ).asTimeMark()

    fun getCommunityPercentageColor(percentage: Double): String = when {
        percentage < 0.01 -> "§a"
        percentage < 0.05 -> "§e"
        percentage < 0.1 -> "§6"
        percentage < 0.25 -> "§6"

        else -> "§c"
    } + percentage.formatPercentage()

    fun getBingoIcon(rank: Int): String {
        val rankIcon = getIcon(rank).orEmpty()
        return if (rank != -1) {
            "$rankIcon $rank"
        } else {
            rankIcon
        }
    }

    @HandleEvent(IslandGraphReloadEvent::class)
    fun onIslandGraphReload() {
        bingoNpcHidden = false
        alixerHidden = false
        checkBingoNpcs()
    }

    // Reset state on every island change in case IslandGraphReloadEvent does not fire for this island (private island, garden)
    @HandleEvent(IslandChangeEvent::class)
    fun onIslandChange() {
        bingoNpcHidden = false
        alixerHidden = false
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!event.repeatSeconds(10)) return
        if (IslandGraphs.currentIslandGraph != null) {
            checkBingoNpcs()
        }
    }

    private fun checkBingoNpcs() {
        if (!IslandType.HUB.isCurrent()) return

        val shouldHideAlixer = !SkyBlockUtils.isBingoProfile
        if (shouldHideAlixer != alixerHidden) {
            alixerHidden = shouldHideAlixer
            IslandGraphs.node("Alixer", GraphNodeTag.NPC).enabled = !shouldHideAlixer
        }

        val shouldHideBingoNpc = !isInBingoEventWindow()
        if (shouldHideBingoNpc != bingoNpcHidden) {
            bingoNpcHidden = shouldHideBingoNpc
            IslandGraphs.node("Bingo", GraphNodeTag.NPC).enabled = !shouldHideBingoNpc
        }
    }

    private fun isInBingoEventWindow(): Boolean {
        val eventStart = getStartOfMonth()
        val now = OffsetDateTime.now(ZoneOffset.UTC).asTimeMark()
        val windowStart = eventStart - BINGO_NPC_OFFSET
        val windowEnd = eventStart + BINGO_EVENT_DURATION + BINGO_NPC_OFFSET
        return now in windowStart..windowEnd
    }
}
