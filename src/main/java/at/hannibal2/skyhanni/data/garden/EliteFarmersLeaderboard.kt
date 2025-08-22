package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.data.garden.FarmingWeight.getWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeight.profileId
import at.hannibal2.skyhanni.data.garden.FarmingWeight.setWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeight.updateCollections
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.isEnabled
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.EnumMap
import kotlin.math.abs
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object EliteFarmersLeaderboard {
    private val config get() = GardenApi.config.eliteFarmingWeights
    private val storage get() = GardenApi.storage?.farmingWeight
    private val lastLeaderboardPos: MutableMap<EliteLeaderboardType, Int>? get() = storage?.lastLeaderboardMap
    private val lastLeaderboardUpdate: MutableMap<EliteLeaderboardType, SimpleTimeMark> = mutableMapOf()
    private val leaderboardWeight: MutableMap<EliteLeaderboardType, Double> = mutableMapOf()
    val loadingLeaderboardMutex = Mutex()
    private val lastApiWeight: EnumMap<EliteLeaderboardType, Double> = enumMapOf()

    private var wasNotLoaded = true

    fun getLeaderboardPosition(leaderboardType: EliteLeaderboardType): Int? {
        if ((lastLeaderboardUpdate[leaderboardType]?.passedSince() ?: INFINITE) < 10.minutes) {
            return lastLeaderboardPos?.get(leaderboardType)
        }
        //ChatUtils.debug("Getting leaderboard position")
        return loadLeaderboardIfAble(leaderboardType)
    }

    private fun loadLeaderboardIfAble(leaderboardType: EliteLeaderboardType): Int? {
        if (loadingLeaderboardMutex.isLocked) return null
        if (profileId == "") updateCollections()
        SkyHanniMod.launchIOCoroutine {
            loadingLeaderboardMutex.withLock {
                val oldPos = lastLeaderboardPos?.get(leaderboardType)
                val lbPos = loadLeaderboardPosition(leaderboardType)
                //ChatUtils.debug("Lbpos: $lbPos")
                lastLeaderboardPos?.set(leaderboardType, lbPos)
                if (wasNotLoaded) checkOffScreenLeaderboardChanges(oldPos, leaderboardType)
                lastLeaderboardUpdate[leaderboardType] = SimpleTimeMark.now()
            }
        }
        //ChatUtils.debug("lastLeaderboardPos: ${lastLeaderboardPos?.get(currentLeaderboardType)}")
        return lastLeaderboardPos?.get(leaderboardType)
    }

    private fun checkOffScreenLeaderboardChanges(oldPosition: Int?, leaderboardType: EliteLeaderboardType) {
        if (!config.showLbChange) return
        if (oldPosition == null) return
        wasNotLoaded = false
        val currentPosition = lastLeaderboardPos?.get(leaderboardType) ?: return

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

    private suspend fun loadLeaderboardPosition(leaderboardType: EliteLeaderboardType): Int {
        //ChatUtils.debug("Loading Leaderboard position")
        // Fetch more upcoming players when the difference between ranks is expected to be tiny
        val currentLeaderboardPos = lastLeaderboardPos?.get(leaderboardType) ?: -1
        val upcomingPlayers = when {
            !isEnabled() -> 0
            currentLeaderboardPos > 10_000 -> 50
            currentLeaderboardPos > 5_000 -> 30
            currentLeaderboardPos > 1_000 -> 20
            else -> 10
        }
        /*// Tell the API to get upcoming players from our local rank (for when new data isn't fetched), or fallback to the
        // provided eta goal rank from the config
        val atRank = when {
            !isEtaEnabled() -> null
            FarmingWeightDisplay.config.useEtaGoalRank.get() && leaderboardPosition != -1 -> min(getRankGoal() + 1, leaderboardPosition)
            FarmingWeightDisplay.config.useEtaGoalRank.get() -> getRankGoal() + 1
            leaderboardPosition != -1 -> leaderboardPosition
            else -> null
        }*/

        //ChatUtils.debug("Calling api data")
        //ChatUtils.debug("profile id: $profileId, lbType: $leaderboardType, upcoming count: $upcomingPlayers")

        val apiData = EliteDevApi.fetchLeaderboardPositions(
            profileId = profileId,
            lbType = leaderboardType,
            upcomingCount = upcomingPlayers,
            atRank = null//atRank,
        ) ?: return currentLeaderboardPos

        val diff = apiData.amount - (getWeight(leaderboardType) ?: -1.0)

        if (diff >= 0 || abs(diff) >= 10) {
            when (leaderboardType) {
                EliteLeaderboardType.ALL_TIME -> updateCollections()
                EliteLeaderboardType.MONTHLY -> setWeight(leaderboardType, apiData.amount)
            }
        }

        /*val newData = apiWeight < apiData.amount
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
        }*/

        // Keep local rank if new data wasn't returned
        //return if (newData) apiData.rank else currentLeaderboardPos
        lastLeaderboardUpdate[leaderboardType] = SimpleTimeMark.now()
        leaderboardWeight[leaderboardType] = apiData.amount
        return apiData.rank
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
