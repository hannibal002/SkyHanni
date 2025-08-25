package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.garden.FarmingWeight.getWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeight.profileId
import at.hannibal2.skyhanni.data.garden.FarmingWeight.setWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeight.updateCollections
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboard
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.UpcomingLeaderboardPlayer
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.isEnabled
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


// TODO fix loading weight profiles + #1 player
@SkyHanniModule
object EliteFarmersLeaderboard {
    val loadingLeaderboardMutex = Mutex()
    private val config get() = GardenApi.config.eliteFarmingWeights
    private val storage get() = GardenApi.storage?.farmingWeight
    private val leaderboardPosMap: MutableMap<EliteLeaderboardType, Int>? get() = storage?.lastLeaderboardMap
    private val minWeight: MutableMap<EliteLeaderboardType, Double>? get() = storage?.minWeight
    private val lastLeaderboardUpdate: MutableMap<EliteLeaderboardType, SimpleTimeMark> = mutableMapOf()
    private val shouldRefreshLeaderboard: MutableMap<EliteLeaderboardType, Boolean> = mutableMapOf()
    private val lastPlayer: MutableMap<EliteLeaderboardType, UpcomingLeaderboardPlayer?> = mutableMapOf()
    private val nextPlayers: MutableMap<EliteLeaderboardType, MutableList<UpcomingLeaderboardPlayer>> = mutableMapOf()
    private val lastApiData: MutableMap<EliteLeaderboardType, EliteLeaderboard> = mutableMapOf()
    private val isUnranked: MutableMap<EliteLeaderboardType, Boolean> = mutableMapOf()

    var apiError = false
    private var hasWarned = false
    private var rankGoal: Int? = null
    private var wasNotLoaded = true
    private var fetchAttempts = 0
    private var lastFetchAttempt = SimpleTimeMark.farPast()

    fun reset() {
        leaderboardPosMap?.clear()
        lastLeaderboardUpdate.clear()
        lastPlayer.clear()
        nextPlayers.clear()
        shouldRefreshLeaderboard.clear()
        isUnranked.clear()
        hasWarned = false
        apiError = false
        hasWarned = false
        rankGoal = null
        fetchAttempts = 0
        lastFetchAttempt = SimpleTimeMark.farPast()
    }
    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.useEtaGoalRank, config.etaGoalRank) {
            shouldRefreshLeaderboard.clear()
            nextPlayers.clear()
            lastPlayer.clear()
            FarmingWeightDisplay.update()
        }
    }

    fun isUnranked(leaderboardType: EliteLeaderboardType): Boolean {
        if (leaderboardType == EliteLeaderboardType.ALL_TIME) return false // We support other methods to calculate all-time farming weight
        return isUnranked[leaderboardType] ?: false
    }

    fun getMinWeight(leaderboardType: EliteLeaderboardType): Double? {
        return minWeight?.get(leaderboardType)
    }

    fun getLeaderboardPosition(leaderboardType: EliteLeaderboardType, override: Boolean = false): Int? {
        val lastUpdate = lastLeaderboardUpdate[leaderboardType]?.passedSince() ?: INFINITE
        val refresh = override || (shouldRefreshLeaderboard[leaderboardType] ?: true)

        if (!refresh && lastUpdate < 10.minutes) {
            val pos = leaderboardPosMap?.get(leaderboardType)
            if (pos != null && pos <= 0) {
                leaderboardPosMap?.remove(leaderboardType)
            } else {
                return pos
            }
        }

        if (lastFetchAttempt.passedSince() <= 5.seconds) return null
        lastFetchAttempt = SimpleTimeMark.now()
        fetchAttempts++

        val pos = loadLeaderboardIfAble(leaderboardType)
        if (pos != null || fetchAttempts > 3) {
            lastLeaderboardUpdate[leaderboardType] = SimpleTimeMark.now()
            shouldRefreshLeaderboard[leaderboardType] = false
            fetchAttempts = 0
        }

        return pos
    }

    fun getNextPlayer(leaderboardType: EliteLeaderboardType): Pair<String, Double>? {
        val weight = getWeight(leaderboardType) ?: return null
        var nextPlayer = nextPlayers[leaderboardType]?.firstOrNull() ?: lastPlayer[leaderboardType] ?: return null
        var weightDiff = nextPlayer.weight - weight
        while (weightDiff < 0) {
            nextPlayer = updateNextPlayer(leaderboardType) ?: break
            weightDiff = nextPlayer.weight - weight
        }
        // This currently doesn't work
        if (leaderboardPosMap?.get(leaderboardType) == 1) {
            val lastPlayer = lastPlayer[leaderboardType]
            if (lastPlayer != null && lastPlayer.weight <= weight) {
                return Pair(lastPlayer.name, weight - lastPlayer.weight)
            }
        }

        return if (weightDiff < 0) null else Pair(nextPlayer.name, weightDiff)
    }

    private fun updateNextPlayer(leaderboardType: EliteLeaderboardType): UpcomingLeaderboardPlayer? {
        val nextPlayer = nextPlayers[leaderboardType]?.firstOrNull() ?: return null
        lastPlayer[leaderboardType] = nextPlayer
        farmingChatMessage("You passed §b${nextPlayer.name} §ein the §6$leaderboardType §eLeaderboard!")
        nextPlayers[leaderboardType]?.removeFirstOrNull() ?: return null

        val currentRank = leaderboardPosMap?.get(leaderboardType) ?: return null
        val rankGoal = getRankGoal(leaderboardType) // getRankGoal returns null if we're at or in front of it
        leaderboardPosMap?.set(leaderboardType, rankGoal ?: (currentRank - 1)) // player we passed should be at rank goal if not null
        return nextPlayers[leaderboardType]?.firstOrNull()
    }

    private fun loadLeaderboardIfAble(leaderboardType: EliteLeaderboardType): Int? {
        if (loadingLeaderboardMutex.isLocked) return null
        if (profileId == "") updateCollections()
        SkyHanniMod.launchIOCoroutine {
            loadingLeaderboardMutex.withLock {
                val oldPos = leaderboardPosMap?.get(leaderboardType)
                val lbPos = loadLeaderboardPosition(leaderboardType)
                if (lbPos != null) {
                    leaderboardPosMap?.set(leaderboardType, lbPos)
                    if (wasNotLoaded) checkOffScreenLeaderboardChanges(oldPos, leaderboardType)
                    lastLeaderboardUpdate[leaderboardType] = SimpleTimeMark.now()
                }
            }
        }
        return leaderboardPosMap?.get(leaderboardType)
    }

    private fun checkOffScreenLeaderboardChanges(oldPosition: Int?, leaderboardType: EliteLeaderboardType) {
        if (!config.showLbChange) return
        if (oldPosition == null) return
        wasNotLoaded = false
        val currentPosition = leaderboardPosMap?.get(leaderboardType) ?: return

        val diff = currentPosition - oldPosition
        if (diff == 0) return
        val verbFormat = if (diff > 0) "§cdropped" else "§arisen"
        val placesFormat = StringUtils.pluralize(abs(diff), "place", withNumber = true)
        farmingChatMessage(
            "§7Since your last visit to the §aGarden§7, " +
                "you have $verbFormat $placesFormat §7on the §d$leaderboardType Leaderboard§7. " +
                "§7(§e#${oldPosition.addSeparators()} §7-> §e#${currentPosition.addSeparators()}§7)",
        )
    }

    private suspend fun loadLeaderboardPosition(leaderboardType: EliteLeaderboardType): Int? {
        if (profileId == "") return null
        // Fetch more upcoming players when the difference between ranks is expected to be tiny
        val currentPos = leaderboardPosMap?.get(leaderboardType) ?: Int.MAX_VALUE
        val upcomingPlayers = getUpcomingPlayerCount(currentPos)
        // Tell the API to get upcoming players from our local rank (for when new data isn't fetched), or fallback to the
        // provided eta goal rank from the config
        val rankGoal = getRankGoal(leaderboardType)
        val useRankGoal = config.useEtaGoalRank.get() && rankGoal != null
        val atRank = getAtRank(currentPos, rankGoal, useRankGoal)

        val apiData = EliteDevApi.fetchLeaderboardPositions(
            profileId = profileId,
            lbType = leaderboardType,
            upcomingCount = upcomingPlayers,
            atRank = atRank,
        ) ?: run {
            ChatUtils.debug("Api error!") // TODO run an actual error
            apiError = true
            return null
        }

        val shouldUpdateData = shouldUpdateData(leaderboardType, apiData)
        // don't update anything besides upcoming players if data hasn't changed since last request
        if (shouldUpdateData) handleDiff(leaderboardType, apiData)
        handleUpcomingPlayers(leaderboardType, apiData, atRank)

        minWeight?.set(leaderboardType, apiData.minAmount)
        lastLeaderboardUpdate[leaderboardType] = SimpleTimeMark.now()
        shouldRefreshLeaderboard[leaderboardType] = false
        apiError = false
        FarmingWeightDisplay.update()
        if (apiData.rank <= 0) {
            isUnranked[leaderboardType] = true
            return null
        }
        return if (shouldUpdateData && currentPos != Int.MAX_VALUE) currentPos else apiData.rank
    }

    private fun getUpcomingPlayerCount(currentPos: Int) = when {
        !isEnabled() -> 0
        currentPos > 10_000 -> 50
        currentPos > 5_000 -> 30
        currentPos > 1_000 -> 20
        else -> 10
    }

    private fun getAtRank(currentPos: Int, rankGoal: Int?, useRankGoal: Boolean): Int? = when {
        currentPos == 1 -> 3
        useRankGoal -> minOf((rankGoal ?: 0) + 1, currentPos)
        currentPos != Int.MAX_VALUE -> currentPos
        else -> null
    }

    private fun shouldUpdateData(leaderboardType: EliteLeaderboardType, apiData: EliteLeaderboard): Boolean {
        val oldApiData = lastApiData[leaderboardType] ?: return true
        val leaderboardDiff = oldApiData.rank != apiData.rank
        val amountDiff = oldApiData.amount != apiData.amount
        return leaderboardDiff || amountDiff
    }

    private fun handleDiff(leaderboardType: EliteLeaderboardType, apiData: EliteLeaderboard) {
        val diff = apiData.amount - (getWeight(leaderboardType) ?: 0.0)
        if ((diff >= 0.5 || abs(diff) >= 10) && apiData.rank != -1) {
            when (leaderboardType) {
                EliteLeaderboardType.ALL_TIME -> updateCollections()
                EliteLeaderboardType.MONTHLY -> setWeight(leaderboardType, apiData.amount)
            }
        }
    }

    private fun handleUpcomingPlayers(
        leaderboardType: EliteLeaderboardType,
        apiData: EliteLeaderboard,
        atRank: Int?
    ) {
        if (apiData.rank == 1 && atRank == 3) {
            lastPlayer[leaderboardType] = apiData.upcomingPlayers.firstOrNull()
        } else {
            nextPlayers[leaderboardType] = mutableListOf()
            apiData.upcomingPlayers.forEach {
                if (it.weight > (getWeight(leaderboardType) ?: apiData.amount)) {
                    nextPlayers[leaderboardType]?.add(it)
                }
            }
        }
    }

    fun getRankGoal(leaderboardType: EliteLeaderboardType): Int? {
        if (!config.useEtaGoalRank.get()) return null
        val value = config.etaGoalRank
        val currentLeaderboardPos = leaderboardPosMap?.get(leaderboardType) ?: Int.MAX_VALUE

        // Check that the provided string is valid
        val goal = value.get().toIntOrNull() ?: 0

        if (goal < 1 || goal >= currentLeaderboardPos) {
            if (goal < 1 && !hasWarned) {
                ChatUtils.chatAndOpenConfig(
                    "Invalid Farming Weight Overtake Goal! Click here to edit the Overtake Goal config value " +
                        "to a positive number less than your current leaderboard position to use this feature!",
                    config::etaGoalRank,
                )
                hasWarned = true
            }
            rankGoal = null
            return null
        }

        if (rankGoal != goal) {
            shouldRefreshLeaderboard[leaderboardType] = true
            rankGoal = goal
        }
        return rankGoal
    }

    private fun farmingChatMessage(message: String) {
        ChatUtils.hoverableChat(
            message,
            listOf(
                "§eClick to open your Farming Weight",
                "§eprofile on §celitebot.dev",
            ),
            "/shfarmingprofile ${PlayerUtils.getName()}",
        )
    }
}
