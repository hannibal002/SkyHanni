package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getLeaderboardConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getLeaderboardRankConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getRankConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getRankGoalIfValid
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteDisplayGenericConfig.LeaderboardTextEntry
import at.hannibal2.skyhanni.data.IslandType
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
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


@SkyHanniModule
object EliteFarmersLeaderboard {
    val loadingLeaderboardMutex = mutableMapOf<KClass<out EliteLeaderboardType>, Mutex>(
        EliteLeaderboardType.Crop::class to Mutex(),
        EliteLeaderboardType.Weight::class to Mutex(),
        EliteLeaderboardType.Pest::class to Mutex()
    )
    private val storage get() = GardenApi.storage?.farmingWeight
    private val leaderboardPosMap: MutableMap<EliteLeaderboardType, Int>? get() = storage?.lastLeaderboardPosMap
    private val leaderboardAmountMap: MutableMap<EliteLeaderboardType, Double>? get() = storage?.leaderboardAmountMap
    private val minAmount: MutableMap<EliteLeaderboardType, Double>? get() = storage?.minAmountMap
    private var lastPassedMessage: SimpleTimeMark = SimpleTimeMark.farPast()
    private val loadedLeaderboardCategories = mutableSetOf<KClass<out EliteLeaderboardType>>()

    private val eliteLeaderboardData: MutableMap<EliteLeaderboardType, EliteLeaderboardData> = mutableMapOf()

    var apiError = false
    private var hasWarned = false
    private var fetchAttempts = 0
    private var lastFetchAttempt = SimpleTimeMark.farPast()

    fun clearEntries(leaderboardType: EliteLeaderboardType) {
        leaderboardPosMap?.remove(leaderboardType)
        eliteLeaderboardData.remove(leaderboardType)
    }

    // removes all subclasses
    fun clearCategories(category: KClass<out EliteLeaderboardType>) {
        leaderboardPosMap?.clearCategory(category)
        eliteLeaderboardData.clearCategory(category)
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
        addPestKill(EliteLeaderboardType.Pest(event.pestType, EliteLeaderboardMode.ALL_TIME))
        addPestKill(EliteLeaderboardType.Pest(event.pestType, EliteLeaderboardMode.MONTHLY))
        addPestKill(EliteLeaderboardType.Pest(null, EliteLeaderboardMode.MONTHLY))
        addPestKill(EliteLeaderboardType.Pest(null, EliteLeaderboardMode.ALL_TIME))
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (lastPassedMessage.passedSince() < 30.seconds) return
        eliteLeaderboardData.forEach { lbtype ->
            if (!getLeaderboardConfig(lbtype.key).showLbChange) return@forEach
            val list = lbtype.value.passedPlayers
            if (list.isEmpty()) return@forEach
            if (list.size < 3) {
                list.forEach { name ->
                    farmingChatMessage("You passed §b$name §ein the §6${lbtype.key} §eLeaderboard!")
                }
            } else {
                farmingChatMessage("You recently passed §b${list.size.addSeparators()} players §ein the §6${lbtype.key} §eLeaderboard!")
            }
            list.clear()
        }
        lastPassedMessage = SimpleTimeMark.now()
    }

    private fun addPestKill(leaderboardType: EliteLeaderboardType, amount: Double = 1.0) {
        leaderboardAmountMap?.set(leaderboardType, (leaderboardAmountMap?.get(leaderboardType) ?: 0.0) + amount)
    }

    fun isUnranked(leaderboardType: EliteLeaderboardType): Boolean {
        return eliteLeaderboardData[leaderboardType]?.isUnranked ?: false
    }

    fun leaderboardMinAmount(leaderboardType: EliteLeaderboardType): Double? {
        return minAmount?.get(leaderboardType)
    }

    fun getLeaderboardPosition(leaderboardType: EliteLeaderboardType, override: Boolean = false): Int? {
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        if (profileId == "") return null // api call requires profile id
        val lastUpdate = lbData.lastUpdate.passedSince()
        val refresh = override || (lbData.shouldRefresh)

        if (!refresh && lastUpdate < 10.minutes) {
            val pos = leaderboardPosMap?.get(leaderboardType)
            if (pos != null && pos <= 0) {
                leaderboardPosMap?.remove(leaderboardType)
            } else {
                return pos
            }
        }

        // We want to prevent spamming the api, especially when swapping leaderboard displays
        if (lastFetchAttempt.passedSince() <= 3.seconds) return null
        lastFetchAttempt = SimpleTimeMark.now()
        fetchAttempts++

        val pos = loadLeaderboardIfAble(leaderboardType)
        if (pos != null || fetchAttempts > 3) {
            lbData.lastUpdate = SimpleTimeMark.now()
            lbData.shouldRefresh = false
            fetchAttempts = 0
        }

        return pos
    }

    fun getNextPlayer(leaderboardType: EliteLeaderboardType): Pair<String, Double>? {
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        val amount = getAmount(leaderboardType) ?: return null
        var nextPlayer = lbData.nextPlayers.firstOrNull() ?: return null
        var amountBehind = nextPlayer.amount - amount
        while (amountBehind < 0) {
            nextPlayer = updateNextPlayer(leaderboardType) ?: break
            amountBehind = nextPlayer.amount - amount
        }
        if (amountBehind < 0) {
            lbData.shouldRefresh = true
            return null
        }
        return Pair(nextPlayer.name, amountBehind)
    }

    fun getLastPlayer(leaderboardType: EliteLeaderboardType): Pair<String, Double>? {
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        val amount = getAmount(leaderboardType) ?: return null
        val lastPlayer = lbData.lastPlayer ?: return null
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
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        val nextPlayer = lbData.nextPlayers.firstOrNull() ?: return null
        lbData.lastPlayer = nextPlayer
        // send messages every ~30s instead of every pass to avoid chat spam
        lbData.passedPlayers.add(nextPlayer.name)
        lbData.nextPlayers.removeFirstOrNull() ?: return null

        val currentRank = leaderboardPosMap?.get(leaderboardType) ?: return null
        // shouldn't be able to pass players if we're rank 1, something went wrong
        if (currentRank == 1) {
            leaderboardPosMap?.remove(leaderboardType)
            lbData.nextPlayers.clear()
            return null
        }
        val rankGoal = getRankGoal(leaderboardType) // getRankGoal returns null if we're at or in front of it
        leaderboardPosMap?.set(leaderboardType, rankGoal ?: (currentRank - 1)) // player we passed should be at rank goal if not null
        return lbData.nextPlayers.firstOrNull()
    }

    private fun loadLeaderboardIfAble(leaderboardType: EliteLeaderboardType): Int? {
        if (loadingLeaderboardMutex[leaderboardType::class]?.isLocked == true) return null
        if (profileId == "") updateCollections()

        val category = leaderboardType::class

        SkyHanniMod.launchIOCoroutine("load elite lb") {
            loadingLeaderboardMutex[leaderboardType::class]?.withLock {
                val oldPos = leaderboardPosMap?.get(leaderboardType)
                val lbPos = loadLeaderboardPosition(leaderboardType)
                lbPos?.let {
                    leaderboardPosMap?.set(leaderboardType, lbPos)
                    // warn for the load of each mode in each enabled display
                    if (category !in loadedLeaderboardCategories) {
                        checkOffScreenLeaderboardChanges(oldPos, leaderboardType)
                        loadedLeaderboardCategories.add(category)
                    }
                    eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }.lastUpdate = SimpleTimeMark.now()
                }
            }
        }
        return leaderboardPosMap?.get(leaderboardType)
    }

    private fun checkOffScreenLeaderboardChanges(oldPosition: Int?, leaderboardType: EliteLeaderboardType) {
        if (!getLeaderboardConfig(leaderboardType).offlineLbChange) return
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
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        if (profileId == "") return null
        // Fetch more upcoming players when the difference between ranks is expected to be tiny
        val currentPos = leaderboardPosMap?.get(leaderboardType) ?: Int.MAX_VALUE
        val upcomingPlayers =
            getUpcomingPlayerCount(currentPos, leaderboardType)
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
        // elite only updates player profiles once an hour, so assume it's wrong if it's the same as last fetch
        val shouldUpdateData = shouldUpdateData(leaderboardType, apiData)
        minAmount?.set(leaderboardType, apiData.minAmount)
        lbData.apiData = apiData
        lbData.lastUpdate = SimpleTimeMark.now()
        lbData.shouldRefresh = false
        apiError = false
        if (apiData.rank <= 0) { // api returns -1 for unranked players
            lbData.isUnranked = true
            // correct wrong data
            leaderboardAmountMap?.remove(leaderboardType)
            leaderboardPosMap?.remove(leaderboardType)
            if (!useRankGoal) {
                lbData.nextPlayers.clear()
                lbData.lastPlayer = null
            }

            return null
        }
        lbData.isUnranked = true
        if (shouldUpdateData) handleDiff(leaderboardType, apiData)
        handleUpcomingPlayers(leaderboardType, apiData)
        // prefer our lb pos
        return if (!shouldUpdateData && currentPos != Int.MAX_VALUE) currentPos else apiData.rank
    }

    private fun getUpcomingPlayerCount(currentPos: Int, leaderboardType: EliteLeaderboardType): Int {
        if (LeaderboardTextEntry.OVERTAKE !in getLeaderboardConfig(leaderboardType).display.text.get()) return 0
        if (leaderboardType.mode == EliteLeaderboardMode.ALL_TIME) {
            return when {
                currentPos > 20_000 -> 100
                currentPos > 10_000 -> 50
                currentPos > 5_000 -> 30
                currentPos > 1_000 -> 20
                else -> 10
            }
        } else if (leaderboardType.mode == EliteLeaderboardMode.MONTHLY) {
            return when {
                currentPos > 5_000 -> 100
                currentPos > 1000 -> 50
                currentPos > 500 -> 30
                currentPos > 100 -> 20
                else -> 10
            }
        }
        return 10
    }

    private fun getAtRank(currentPos: Int, rankGoal: Int?, useRankGoal: Boolean): Int? = when {
        useRankGoal -> minOf((rankGoal ?: 0) + 1, currentPos)
        currentPos != Int.MAX_VALUE -> currentPos
        else -> null
    }

    // only update data if api data has changed since last request
    private fun shouldUpdateData(leaderboardType: EliteLeaderboardType, apiData: EliteLeaderboard): Boolean {
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        val oldApiData = lbData.apiData ?: return true
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
        if (diff >= 0.5 || abs(diff) >= 100) {
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
        if (diffWeight >= 0.5 || abs(diffWeight) >= 100) {
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
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        lbData.lastPlayer = apiData.previous?.firstOrNull()
        lbData.nextPlayers.clear()
        apiData.upcomingPlayers.forEach {
            if (apiData.rank != 1) lbData.nextPlayers.add(it)
        }
    }

    fun getRankGoal(leaderboardType: EliteLeaderboardType): Int? {
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
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
            lbData.rankGoal = null
            return null
        }

        if (lbData.rankGoal != goal) {
            lbData.shouldRefresh = true
            lbData.rankGoal = goal
        }

        return lbData.rankGoal
    }

    fun reset() {
        leaderboardPosMap?.clear()
        leaderboardAmountMap?.clear()
        eliteLeaderboardData.clear()
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

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("elite leaderboard")
        event.addIrrelevant {
            eliteLeaderboardData.forEach {
                add(it.value.apiData.toString())
            }
        }
    }
}
