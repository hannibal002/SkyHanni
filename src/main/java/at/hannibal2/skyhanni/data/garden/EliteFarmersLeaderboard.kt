package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.apiWeight
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.checkOffScreenLeaderboardChanges
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.config
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.displayWeight
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.farmingChatMessage
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.getRankGoal
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.isEnabled
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.isEtaEnabled
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.lastLeaderboardUpdate
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.lbName
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.leaderboardPosition
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.loadLeaderboardPosition
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.loadingLeaderboardMutex
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.minAmount
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.nextPlayers
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.profileId
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.shWeightDiff
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.storage
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.weight
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.EnumMap
import kotlin.math.abs
import kotlin.math.min

@SkyHanniModule
object EliteFarmersLeaderboard {
    private val config get() = GardenApi.config.eliteFarmingWeights
    private val storage = GardenApi.storage?.farmingWeight
    private val leaderboardPosMap: EnumMap<EliteLeaderboardType, Int> = enumMapOf()
    private val lastLeaderboardUpdate: EnumMap<EliteLeaderboardType, SimpleTimeMark> = enumMapOf()
    private val loadingLeaderboardMutex = Mutex()
    private val lastApiWeight: EnumMap<EliteLeaderboardType, Double> = enumMapOf()

    var currentLeaderboardType: EliteLeaderboardType = storage. EliteLeaderboardType.NORMAL

    private fun loadLeaderboardIfAble() {
        if (loadingLeaderboardMutex.isLocked) return
        SkyHanniMod.launchIOCoroutine {
            loadingLeaderboardMutex.withLock {
                val wasNotLoaded = leaderboardPosMap.isEmpty()
                leaderboardPosMap[currentLeaderboardType] = loadLeaderboardPosition()
                if (wasNotLoaded) checkOffScreenLeaderboardChanges()
                storage?.lastLeaderboard = leaderboardPosMap
                lastLeaderboardUpdate[currentLeaderboardType] = SimpleTimeMark.now()
            }
        }
    }

    private fun checkOffScreenLeaderboardChanges() {
        if (!config.showLbChange) return
        val oldPosition = storage?.lastLeaderboard ?: return
        if (oldPosition <= 0 || leaderboardPosition <= 0) return

        val diff = leaderboardPosition - oldPosition
        if (diff == 0) return
        val verbFormat = if (diff > 0) "§cdropped" else "§arisen"
        val placesFormat = StringUtils.pluralize(abs(diff), "place", withNumber = true)
        farmingChatMessage(
            "§7Since your last visit to the §aGarden§7, " +
                "you have $verbFormat $placesFormat §7on the §d$lbName Leaderboard§7. " +
                "§7(§e#${oldPosition.addSeparators()} §7-> §e#${leaderboardPosition.addSeparators()}§7)",
        )
    }

    private suspend fun loadLeaderboardPosition(): Int {
        // Fetch more upcoming players when the difference between ranks is expected to be tiny
        val currentLeaderboardPos = leaderboardPosMap[currentLeaderboardType] ?: -1
        val upcomingPlayers = when {
            !isEnabled() -> 0
            currentLeaderboardPos > 10_000 -> 50
            currentLeaderboardPos > 5_000 -> 30
            currentLeaderboardPos > 1_000 -> 20
            else -> 10
        }
        // Tell the API to get upcoming players from our local rank (for when new data isn't fetched), or fallback to the
        // provided eta goal rank from the config
        val atRank = when {
            !isEtaEnabled() -> null
            FarmingWeightDisplay.config.useEtaGoalRank.get() && leaderboardPosition != -1 -> min(getRankGoal() + 1, leaderboardPosition)
            FarmingWeightDisplay.config.useEtaGoalRank.get() -> getRankGoal() + 1
            leaderboardPosition != -1 -> leaderboardPosition
            else -> null
        }

        val apiData = EliteDevApi.fetchLeaderboardPositions(
            profileId = FarmingWeight.profileId,
            lbType = currentLeaderboardType,
            upcomingCount = upcomingPlayers,
            atRank = atRank,
        ) ?: return currentLeaderboardPos

        val newData = apiWeight < apiData.amount
        minAmount = apiData.minAmount

        if (newData) {
            shWeightDiff = weight - apiData.amount
            apiWeight = apiData.amount
        }

        // Reset weight diff if not a monthly leaderboard
        if (apiData.initialAmount == 0.0) {
            shWeightDiff = 0.0
        }

        if (isEtaEnabled()) {
            nextPlayers.clear()
            apiData.upcomingPlayers.forEach {
                if (it.weight > displayWeight) {
                    nextPlayers.add(it)
                }
            }
        }

        // Keep local rank if new data wasn't returned
        return if (newData) apiData.rank else currentLeaderboardPos
    }
}
