package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getLeaderboardConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getLeaderboardRankConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getRankConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getRankGoalIfValid
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteDisplayGenericConfig.LeaderboardTextEntry
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.getCollection
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.getFactor
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.getWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.profileId
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.setWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.updateCollections
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboard
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardPlayer
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.crop
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


@SkyHanniModule
object EliteFarmersLeaderboard {
    val loadingLeaderboardMutex = Mutex()
    private val storage get() = GardenApi.storage?.farmingWeight
    private val leaderboardPosMap: MutableMap<EliteLeaderboardType, Int>? get() = storage?.lastLeaderboardPosMap
    private val leaderboardAmountMap: MutableMap<EliteLeaderboardType, Double>? get() = storage?.leaderboardAmountMap
    private val minAmount: MutableMap<EliteLeaderboardType, Double>? get() = storage?.minAmountMap
    private val lastLeaderboardUpdate: MutableMap<EliteLeaderboardType, SimpleTimeMark> = mutableMapOf()
    private val shouldRefreshLeaderboard: MutableMap<EliteLeaderboardType, Boolean> = mutableMapOf()
    private val lastPlayer: MutableMap<EliteLeaderboardType, EliteLeaderboardPlayer?> = mutableMapOf()
    private val nextPlayers: MutableMap<EliteLeaderboardType, MutableList<EliteLeaderboardPlayer>> = mutableMapOf()
    private val lastApiData: MutableMap<EliteLeaderboardType, EliteLeaderboard> = mutableMapOf()
    private val isUnranked: MutableMap<EliteLeaderboardType, Boolean> = mutableMapOf()
    private val rankGoal: MutableMap<EliteLeaderboardType, Int?> = mutableMapOf()
    private val loadedLeaderboardCategories = mutableSetOf<KClass<out EliteLeaderboardType>>()

    var apiError = false
    private var hasWarned = false
    private var fetchAttempts = 0
    private var lastFetchAttempt = SimpleTimeMark.farPast()

    fun clearEntries(leaderboardType: EliteLeaderboardType) {
        rankGoal.remove(leaderboardType)
        nextPlayers.remove(leaderboardType)
        leaderboardPosMap?.remove(leaderboardType)
        shouldRefreshLeaderboard.remove(leaderboardType)
        nextPlayers.remove(leaderboardType)
        lastPlayer.remove(leaderboardType)
    }

    fun clearCategories(category: KClass<out EliteLeaderboardType>) {
        rankGoal.clearCategory(category)
        nextPlayers.clearCategory(category)
        leaderboardPosMap?.clearCategory(category)
        shouldRefreshLeaderboard.clearCategory(category)
        nextPlayers.clearCategory(category)
        lastPlayer.clearCategory(category)
    }

    private fun <T> MutableMap<EliteLeaderboardType, T>.clearCategory(category: KClass<out EliteLeaderboardType>) {
        val keysToRemove = keys.filter { category.isInstance(it) }
        keysToRemove.forEach { remove(it) }
    }

    @HandleEvent
    fun onCropCollectionAdd(event: CropCollectionAddEvent) {
        if (event.cropCollectionType == CropCollectionType.UNKNOWN) return
        val leaderboardType = EliteLeaderboardType.Crop(event.crop, EliteLeaderboardMode.MONTHLY)
        val currentAmount = leaderboardAmountMap?.get(leaderboardType) ?: event.amount.toDouble()
        leaderboardAmountMap?.set(leaderboardType, currentAmount + event.amount.toDouble())
    }

    @HandleEvent
    fun onPestKill(event: PestKillEvent) {
        addPestKill(EliteLeaderboardType.Pest(event.pest, EliteLeaderboardMode.ALL_TIME))
        addPestKill(EliteLeaderboardType.Pest(null, EliteLeaderboardMode.MONTHLY))
        addPestKill(EliteLeaderboardType.Pest(null, EliteLeaderboardMode.ALL_TIME))
    }

    private fun addPestKill(leaderboardType: EliteLeaderboardType, amount: Double = 1.0) {
        leaderboardAmountMap?.set(leaderboardType, (leaderboardAmountMap?.get(leaderboardType) ?: 0.0) + amount)
    }

    fun isUnranked(leaderboardType: EliteLeaderboardType): Boolean {
        return isUnranked[leaderboardType] ?: false
    }

    fun leaderboardMinAmount(leaderboardType: EliteLeaderboardType): Double? {
        return minAmount?.get(leaderboardType)
    }

    fun getLeaderboardPosition(leaderboardType: EliteLeaderboardType, override: Boolean = false): Int? {
        if (profileId == "") return null // api call requires profile id
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

        // We want to prevent spamming the api, especially when swapping leaderboard displays
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
        val amount = getAmount(leaderboardType) ?: return null
        var nextPlayer = nextPlayers[leaderboardType]?.firstOrNull() ?: return null
        var amountBehind = nextPlayer.amount - amount
        while (amountBehind < 0) {
            nextPlayer = updateNextPlayer(leaderboardType) ?: break
            amountBehind = nextPlayer.amount - amount
        }
        if (amountBehind < 0) {
            shouldRefreshLeaderboard[leaderboardType] = true
            return null
        }
        return Pair(nextPlayer.name, amountBehind)
    }

    fun getLastPlayer(leaderboardType: EliteLeaderboardType): Pair<String, Double>? {
        val amount = getAmount(leaderboardType) ?: return null
        val lastPlayer = lastPlayer[leaderboardType] ?: return null
        val amountAhead = amount - lastPlayer.amount
        return if (amountAhead < 0) null else Pair(lastPlayer.name, amountAhead)
    }

    fun getAmount(leaderboardType: EliteLeaderboardType): Double? {
        return when (leaderboardType) {
            is EliteLeaderboardType.Weight -> getWeight(leaderboardType.mode)
            is EliteLeaderboardType.Crop -> getCropCollection(leaderboardType.crop, leaderboardType.mode)
            else -> leaderboardAmountMap?.get(leaderboardType)
        }
    }

    fun getAmount(leaderboardType: EliteLeaderboardType, eliteLeaderboardMode: EliteLeaderboardMode): Double? {
        return when (leaderboardType) {
            is EliteLeaderboardType.Weight -> getAmount(
                leaderboardType.copy(mode = eliteLeaderboardMode)
            )
            is EliteLeaderboardType.Crop -> getAmount(
                leaderboardType.copy(mode = eliteLeaderboardMode)
            )
            is EliteLeaderboardType.Pest -> getAmount(
                leaderboardType.copy(mode = eliteLeaderboardMode)
            )
        }
    }

    private fun getCropCollection(crop: CropType, leaderboardMode: EliteLeaderboardMode): Double? {
        return when (leaderboardMode) {
            EliteLeaderboardMode.ALL_TIME -> crop.getCollection().toDouble()
            EliteLeaderboardMode.MONTHLY -> leaderboardAmountMap?.get(EliteLeaderboardType.Crop(crop, EliteLeaderboardMode.MONTHLY))
        }
    }

    private fun updateNextPlayer(leaderboardType: EliteLeaderboardType): EliteLeaderboardPlayer? {
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

        val category = leaderboardType::class

        SkyHanniMod.launchIOCoroutine {
            loadingLeaderboardMutex.withLock {
                val oldPos = leaderboardPosMap?.get(leaderboardType)
                val lbPos = loadLeaderboardPosition(leaderboardType)
                lbPos?.let {
                    leaderboardPosMap?.set(leaderboardType, lbPos)
                    // warn for the load of each mode in each enabled display
                    if (category !in loadedLeaderboardCategories) {
                        checkOffScreenLeaderboardChanges(oldPos, leaderboardType)
                        loadedLeaderboardCategories.add(category)
                    }
                    lastLeaderboardUpdate[leaderboardType] = SimpleTimeMark.now()
                }
            }
        }
        return leaderboardPosMap?.get(leaderboardType)
    }

    private fun checkOffScreenLeaderboardChanges(oldPosition: Int?, leaderboardType: EliteLeaderboardType) {
        if (!getLeaderboardConfig(leaderboardType).showLbChange) return
        if (oldPosition == null) return
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
        val upcomingPlayers = getUpcomingPlayerCount(currentPos, leaderboardType)
        // Fetch upcoming players from current lb pos if api hasn't updated, or from rank goal
        val rankGoal = getRankGoal(leaderboardType)
        val useRankGoal = getRankConfig(leaderboardType).useRankGoal.get() && rankGoal != null
        val atRank = getAtRank(currentPos, rankGoal, useRankGoal)

        val apiData = EliteDevApi.fetchLeaderboardPositions(
            profileId = profileId,
            lbType = leaderboardType,
            upcomingCount = upcomingPlayers,
            atRank = atRank,
        )

        val shouldUpdateData = shouldUpdateData(leaderboardType, apiData)
        // don't update anything besides upcoming players if data hasn't changed since last request
        if (shouldUpdateData) handleDiff(leaderboardType, apiData)
        handleUpcomingPlayers(leaderboardType, apiData)

        minAmount?.set(leaderboardType, apiData.minAmount)
        lastApiData[leaderboardType] = apiData
        lastLeaderboardUpdate[leaderboardType] = SimpleTimeMark.now()
        shouldRefreshLeaderboard[leaderboardType] = false // Don't want to fetch again if api call was successful
        apiError = false
        if (apiData.rank <= 0) { // api returns -1 for unranked players
            isUnranked[leaderboardType] = true
            return null
        }
        // api caches data, so prefer our lb pos if api pos hasn't changed since last request
        return if (!shouldUpdateData && currentPos != Int.MAX_VALUE) currentPos else apiData.rank
    }

    private fun getUpcomingPlayerCount(currentPos: Int, leaderboardType: EliteLeaderboardType): Int {
        if (LeaderboardTextEntry.OVERTAKE !in getLeaderboardConfig(leaderboardType).display.text.get()) return 0
        if (leaderboardType.mode == EliteLeaderboardMode.ALL_TIME) {
            return when {
                currentPos > 10_000 -> 50
                currentPos > 5_000 -> 30
                currentPos > 1_000 -> 20
                else -> 10
            }
        } else if (leaderboardType.mode == EliteLeaderboardMode.MONTHLY) {
            return when {
                currentPos > 1000 -> 50
                currentPos > 500 -> 30
                currentPos > 100 -> 20
                else -> 10
            }
        }
        return 10
    }

    private fun getAtRank(currentPos: Int, rankGoal: Int?, useRankGoal: Boolean): Int? = when {
        currentPos == 1 -> 3
        useRankGoal -> minOf((rankGoal ?: 0) + 1, currentPos)
        currentPos != Int.MAX_VALUE -> currentPos
        else -> null
    }

    // only update data if api data has changed since last request
    private fun shouldUpdateData(leaderboardType: EliteLeaderboardType, apiData: EliteLeaderboard): Boolean {
        val oldApiData = lastApiData[leaderboardType] ?: return true
        val amountDiff = oldApiData.amount != apiData.amount
        return amountDiff
    }

    private fun handleDiff(leaderboardType: EliteLeaderboardType, apiData: EliteLeaderboard) {
        if (apiData.rank == -1) return // no lb rank means amount is invalid
        val diff = apiData.amount - (getAmount(leaderboardType) ?: 0.0)
        when (leaderboardType) {
            is EliteLeaderboardType.Weight -> handleWeightDiff(leaderboardType, apiData, diff)
            is EliteLeaderboardType.Crop -> handleCollectionDiff(leaderboardType, apiData, diff)
            is EliteLeaderboardType.Pest -> handlePestDiff(leaderboardType, apiData, diff)
        }
    }

    private fun handleWeightDiff(
        leaderboardType: EliteLeaderboardType,
        apiData: EliteLeaderboard,
        diff: Double
    ) {
        if (diff >= 0.5 || abs(diff) >= 30) {
            when (leaderboardType.mode) {
                EliteLeaderboardMode.ALL_TIME -> updateCollections() // we handle all-time weight in the farmingweight class
                EliteLeaderboardMode.MONTHLY -> setWeight(leaderboardType.mode, apiData.amount)
            }
        }
    }

    private fun handleCollectionDiff(
        leaderboardType: EliteLeaderboardType,
        apiData: EliteLeaderboard,
        diff: Double
    ) {
        val crop = leaderboardType.crop ?: return
        val diffWeight = diff / crop.getFactor()
        if (diffWeight >= 0.5 || abs(diffWeight) >= 30) {
            when (leaderboardType.mode) {
                EliteLeaderboardMode.ALL_TIME -> updateCollections() // we handle all-time collections in the farming weight class
                EliteLeaderboardMode.MONTHLY ->
                    leaderboardAmountMap?.set(leaderboardType, apiData.amount)
            }
        }
    }

    private fun handlePestDiff(
        leaderboardType: EliteLeaderboardType,
        apiData: EliteLeaderboard,
        diff: Double
    ) {
        if (diff >= 1 || abs(diff) >= 150) {
            leaderboardAmountMap?.set(leaderboardType, apiData.amount)
        }
    }

    private fun handleUpcomingPlayers(
        leaderboardType: EliteLeaderboardType,
        apiData: EliteLeaderboard,
    ) {
        lastPlayer[leaderboardType] = apiData.previous?.firstOrNull()
        nextPlayers[leaderboardType] = mutableListOf()
        apiData.upcomingPlayers.forEach {
            if (apiData.rank != 1) nextPlayers[leaderboardType]?.add(it)
        }
    }

    fun getRankGoal(leaderboardType: EliteLeaderboardType): Int? {
        val goal = getRankGoalIfValid(leaderboardType)?.get()?.toIntOrNull() ?: return null

        val currentLeaderboardPos = leaderboardPosMap?.get(leaderboardType) ?: Int.MAX_VALUE

        if (goal < 1 || goal >= currentLeaderboardPos) {
            if (goal < 1 && !hasWarned) {
                getLeaderboardRankConfig(leaderboardType)?.let { prop ->
                    ChatUtils.chatAndOpenConfig(
                        "Invalid $leaderboardType Rank Goal! Click here to edit the Rank Goal config value " +
                            "to a positive number less than your current leaderboard position to use this feature!",
                        prop,
                    )
                }
                hasWarned = true
            }
            rankGoal[leaderboardType] = null
            return null
        }

        if (rankGoal[leaderboardType] != goal) {
            shouldRefreshLeaderboard[leaderboardType] = true
            rankGoal[leaderboardType] = goal
        }

        return rankGoal[leaderboardType]
    }

    fun reset() {
        leaderboardPosMap?.clear()
        leaderboardAmountMap?.clear()
        lastLeaderboardUpdate.clear()
        lastPlayer.clear()
        nextPlayers.clear()
        shouldRefreshLeaderboard.clear()
        rankGoal.clear()
        isUnranked.clear()
        apiError = false
        hasWarned = false
        fetchAttempts = 0
        lastFetchAttempt = SimpleTimeMark.farPast()
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
