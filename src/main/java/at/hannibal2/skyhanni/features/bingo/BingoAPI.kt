package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.config.Storage.PlayerSpecific.BingoSession
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoRanksJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.bingo.card.goals.BingoGoal
import at.hannibal2.skyhanni.features.bingo.card.goals.GoalType
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

object BingoAPI {
    private var ranks = mapOf<String, Int>()
    private var data: Map<String, BingoJson.BingoData> = emptyMap()

    val bingoGoals get() = bingoStorage.goals
    val personalGoals get() = bingoGoals.values.filter { it.type == GoalType.PERSONAL }
    val communityGoals get() = bingoGoals.values.filter { it.type == GoalType.COMMUNITY }
    var lastBingoCardOpenTime = SimpleTimeMark.farPast()

    private val detectionPattern by RepoPattern.pattern("bingo.detection.scoreboard", " §.Ⓑ §.Bingo")

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        ranks = event.getConstant<BingoRanksJson>("BingoRanks").ranks
        data = event.getConstant<BingoJson>("Bingo").bingo_tips
    }

    fun getRankFromScoreboard(text: String) = if (detectionPattern.matches(text)) getRank(text) else null

    fun getRank(text: String) = ranks.entries.find { text.contains(it.key) }?.value

    fun getIcon(searchRank: Int) = ranks.entries.find { it.value == searchRank }?.key

    // We added the suffix (Community Goal) so that older skyhanni versions don't crash with the new repo data.
    fun getData(itemName: String) =
        data.filter { itemName.startsWith(it.key.split(" (Community Goal)")[0]) }.values.firstOrNull()

    fun BingoGoal.getData(): BingoJson.BingoData? = if (type == GoalType.COMMUNITY) {
        getData(displayName)
    } else {
        data[displayName]
    }

    val bingoStorage: BingoSession by lazy {
        val playerSpecific = ProfileStorageData.playerSpecific ?: error("playerSpecific is null")
        playerSpecific.bingoSessions.getOrPut(getStartOfMonthInMillis()) { BingoSession() }
    }

    private fun getStartOfMonthInMillis() = OffsetDateTime.of(
        TimeUtils.getCurrentLocalDate().plusDays(5).withDayOfMonth(1),
        LocalTime.MIDNIGHT, ZoneOffset.UTC
    ).toEpochSecond()

    fun getCommunityPercentageColor(percentage: Double): String = when {
        percentage < 0.01 -> "§a"
        percentage < 0.05 -> "§e"
        percentage < 0.1 -> "§6"
        percentage < 0.25 -> "§6"

        else -> "§c"
    } + LorenzUtils.formatPercentage(percentage)
}
