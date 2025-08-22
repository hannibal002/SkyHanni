package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.data.garden.FarmingWeight.getWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeight.profileId
import at.hannibal2.skyhanni.data.garden.FarmingWeight.setWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeight.updateCollections
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.UpcomingLeaderboardPlayer
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay.isEnabled
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object EliteFarmersLeaderboard {
    val loadingLeaderboardMutex = Mutex()
    private val config get() = GardenApi.config.eliteFarmingWeights
    private val storage get() = GardenApi.storage?.farmingWeight
    private val leaderboardPosMap: MutableMap<EliteLeaderboardType, Int>? get() = storage?.lastLeaderboardMap
    private val lastLeaderboardUpdate: MutableMap<EliteLeaderboardType, SimpleTimeMark> = mutableMapOf()
    private val leaderboardWeight: MutableMap<EliteLeaderboardType, Double> = mutableMapOf()
    private val lastApiWeight: MutableMap<EliteLeaderboardType, Double> = mutableMapOf()
    private val nextPlayers: MutableMap<EliteLeaderboardType, MutableList<UpcomingLeaderboardPlayer>> = mutableMapOf()

    private var wasNotLoaded = true

    fun getLeaderboardPosition(leaderboardType: EliteLeaderboardType): Int? {
        if ((lastLeaderboardUpdate[leaderboardType]?.passedSince() ?: INFINITE) < 10.minutes) {
            return leaderboardPosMap?.get(leaderboardType)
        }
        return loadLeaderboardIfAble(leaderboardType)
    }

    fun getNextPlayer(leaderboardType: EliteLeaderboardType): Pair<String, Double>? {
        var nextPlayer = nextPlayers[leaderboardType]?.firstOrNull() ?: return null
        val weight = getWeight(leaderboardType) ?: return null
        var weightDiff = nextPlayer.weight - weight
        while (weightDiff < 0) {
            nextPlayer = updateNextPlayer(leaderboardType) ?: return null
            weightDiff = nextPlayer.weight - weight
        }
        return Pair(nextPlayer.name, weightDiff)
    }

    private fun updateNextPlayer(leaderboardType: EliteLeaderboardType): UpcomingLeaderboardPlayer? {
        val nextPlayer = nextPlayers[leaderboardType]?.firstOrNull() ?: return null
        farmingChatMessage("You passed §b${nextPlayer.name} §ein the §6$leaderboardType §eLeaderboard!")
        nextPlayers[leaderboardType]?.removeFirstOrNull() ?: return null

        val currentRank = leaderboardPosMap?.get(leaderboardType) ?: return null
        leaderboardPosMap?.set(leaderboardType, currentRank - 1)
        return nextPlayers[leaderboardType]?.firstOrNull()
    }

    private fun loadLeaderboardIfAble(leaderboardType: EliteLeaderboardType): Int? {
        if (loadingLeaderboardMutex.isLocked) return null
        if (profileId == "") updateCollections()
        SkyHanniMod.launchIOCoroutine {
            loadingLeaderboardMutex.withLock {
                val oldPos = leaderboardPosMap?.get(leaderboardType)
                val lbPos = loadLeaderboardPosition(leaderboardType)
                leaderboardPosMap?.set(leaderboardType, lbPos)
                if (wasNotLoaded) checkOffScreenLeaderboardChanges(oldPos, leaderboardType)
                lastLeaderboardUpdate[leaderboardType] = SimpleTimeMark.now()
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

    private suspend fun loadLeaderboardPosition(leaderboardType: EliteLeaderboardType): Int {
        // Fetch more upcoming players when the difference between ranks is expected to be tiny
        val currentLeaderboardPos = leaderboardPosMap?.get(leaderboardType) ?: -1
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

        // TODO config load event
        if (config.upcomingPlayers) {
            nextPlayers[leaderboardType] = mutableListOf()
            val weight = getWeight(leaderboardType) ?: 0.0
            apiData.upcomingPlayers.forEach {
                if (it.weight > weight) {
                    nextPlayers[leaderboardType]?.add(it)
                }
            }
        }
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
