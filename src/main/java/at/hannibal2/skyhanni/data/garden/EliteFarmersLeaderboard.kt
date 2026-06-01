package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getLeaderboardConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getLeaderboardRankConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getRankConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getRankGoalIfValid
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteDisplayGenericConfig.LeaderboardTextEntry
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.setCollectionCounter
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.getCollection
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.getWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.profileId
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.setWeight
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboard
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardPlayer
import at.hannibal2.skyhanni.data.foraging.ForagingCollectionApi
import at.hannibal2.skyhanni.data.foraging.ForagingDebugLog
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.events.foraging.ForagingCollectionAddEvent
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.crop
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.foragingLog
import at.hannibal2.skyhanni.features.foraging.ForagingLogType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.events.garden.pests.PestKillEvent
import at.hannibal2.skyhanni.features.achievements.AchievementManager
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.misc.ContributorManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.withColor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import kotlin.math.abs
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


@SkyHanniModule
object EliteFarmersLeaderboard {
    val loadingLeaderboardMutex = mutableMapOf<KClass<out EliteLeaderboardType>, Mutex>(
        EliteLeaderboardType.Crop::class to Mutex(),
        EliteLeaderboardType.Weight::class to Mutex(),
        EliteLeaderboardType.Pest::class to Mutex(),
        EliteLeaderboardType.ForagingLog::class to Mutex(),
    )
    private val storage get() = GardenApi.storage?.farmingWeight

    data class LeaderboardPlayerInfo(
        val name: String,
        val amountUntil: Double,
        val rank: Int?,
    )

    private val leaderboardPosMap: MutableMap<EliteLeaderboardType, Int>? get() = storage?.lastLeaderboardPosMap
    private val leaderboardAmountMap: MutableMap<EliteLeaderboardType, Double>? get() = storage?.leaderboardAmountMap
    private val minAmount: MutableMap<EliteLeaderboardType, Double>? get() = storage?.minAmountMap
    private var lastPassedMessage: SimpleTimeMark = SimpleTimeMark.farPast()
    private val loadedLeaderboardCategories = mutableSetOf<KClass<out EliteLeaderboardType>>()

    private val eliteLeaderboardData: MutableMap<EliteLeaderboardType, EliteLeaderboardData> = mutableMapOf()

    var apiError = false
    var apiUnavailable = false
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
    fun onForagingCollectionAdd(event: ForagingCollectionAddEvent) {
        // ALL_TIME: getForagingLogCollection reads the local storage counter directly, so it updates in
        // real time as ForagingCollectionApi.recordLogGain writes to storage before firing this event.
        // No leaderboardAmountMap update needed here for ALL_TIME.

        // MONTHLY: accumulate the session delta on top of the API baseline so the displayed count
        // decreases in real time while the player forages.
        // Guard with ?: return so we only accumulate once the Elite API has provided an initial monthly
        // amount — starting from 0 would show a misleadingly large "until next player" value.
        val monthlyType = EliteLeaderboardType.ForagingLog(event.logType, EliteLeaderboardMode.MONTHLY)
        val monthlyCurrent = leaderboardAmountMap?.get(monthlyType) ?: return
        leaderboardAmountMap?.set(monthlyType, monthlyCurrent + event.amount.toDouble())
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
        eliteLeaderboardData.forEach { lbType ->
            if (!getLeaderboardConfig(lbType.key).showLbChange) return@forEach
            val list = lbType.value.passedPlayers
            if (list.isEmpty()) return@forEach
            if (list.size < 3) {
                list.forEach { name ->
                    farmingChatMessage(
                        componentBuilder {
                            append("You passed ")
                            append(name) {
                                withColor(ChatFormatting.AQUA)
                            }
                            val contribUUID = ContributorManager.getUUIDFromDisplayName(name)
                            if (contribUUID != null) {
                                AchievementManager.completeAchievement(BETTER_THAN_DEV_ACHIEVEMENT)
                                ContributorManager.getSuffix(contribUUID)?.let {
                                    append(" ")
                                    append(it) {
                                        withColor(ChatFormatting.WHITE)
                                    }
                                }
                            }
                            append(" in the ")
                            append("${lbType.key}") {
                                withColor(ChatFormatting.GOLD)
                            }
                            append(" Leaderboard!")
                            withColor(ChatFormatting.YELLOW)
                        }
                    )
                }
            } else {
                farmingChatMessage("You recently passed §b${list.size.addSeparators()} players §ein the §6${lbType.key} §eLeaderboard!")
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

        // We want to prevent spamming the api, especially when swapping leaderboard displays.
        // However, allow an immediate fetch when this leaderboard type has never been loaded this session
        // (apiData == null) — e.g. when the player switches to a new tree type mid-session.
        // The throttle only applies to re-fetches of already-seen types.
        val neverFetchedThisSession = lbData.apiData == null
        if (!neverFetchedThisSession && lastFetchAttempt.passedSince() <= 3.seconds) return null
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

    fun getNextPlayer(leaderboardType: EliteLeaderboardType): LeaderboardPlayerInfo? {
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
        return LeaderboardPlayerInfo(nextPlayer.name, amountBehind, nextPlayer.rank)
    }

    fun getLastPlayer(leaderboardType: EliteLeaderboardType): LeaderboardPlayerInfo? {
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        val amount = getAmount(leaderboardType) ?: return null
        val lastPlayer = lbData.lastPlayer ?: return null
        val amountAhead = amount - lastPlayer.amount
        return if (amountAhead < 0) null else LeaderboardPlayerInfo(lastPlayer.name, amountAhead, lastPlayer.rank)
    }

    fun getAmount(leaderboardType: EliteLeaderboardType): Double? {
        return when (leaderboardType) {
            is EliteLeaderboardType.Weight -> getWeight(leaderboardType.mode)
            is EliteLeaderboardType.Crop -> getCropCollection(leaderboardType.crop, leaderboardType.mode)
            is EliteLeaderboardType.ForagingLog -> getForagingLogCollection(leaderboardType.log, leaderboardType.mode)
            else -> leaderboardAmountMap?.get(leaderboardType)
        }
    }

    private fun getForagingLogCollection(logType: ForagingLogType, mode: EliteLeaderboardMode): Double {
        // Optimistic block-break estimates that make the counter decrement immediately on each
        // axe swing instead of waiting for the sack/inventory packet to arrive.
        // Long (whole numbers) so the display never shows fractional log counts.
        val optimistic = ForagingCollectionApi.optimisticGains[logType] ?: 0L
        return when (mode) {
            // Mirror getCropCollection: always return local count so getNextPlayer() can compute
            // amountBehind even for unranked players (where leaderboardAmountMap is cleared).
            EliteLeaderboardMode.ALL_TIME -> {
                val localCount = with(ForagingCollectionApi) { logType.getCollection() }
                (localCount + optimistic).toDouble()
            }
            // Fall back to 0.0 when the base is null (unranked player, or first load before API
            // responds). This lets getNextPlayer() find the last-ranked player to show as the
            // overtake target, and lets onForagingCollectionAdd accumulate gains once the base
            // has been initialised to 0.0 by the unranked handler below.
            EliteLeaderboardMode.MONTHLY -> {
                val base = leaderboardAmountMap?.get(EliteLeaderboardType.ForagingLog(logType, mode)) ?: 0.0
                base + optimistic
            }
        }
    }

    fun getAmount(leaderboardType: EliteLeaderboardType, eliteLeaderboardMode: EliteLeaderboardMode): Double? {
        return when (leaderboardType) {
            is EliteLeaderboardType.Weight -> getAmount(
                leaderboardType.copy(mode = eliteLeaderboardMode),
            )

            is EliteLeaderboardType.Crop -> getAmount(
                leaderboardType.copy(mode = eliteLeaderboardMode),
            )

            is EliteLeaderboardType.Pest -> getAmount(
                leaderboardType.copy(mode = eliteLeaderboardMode),
            )

            is EliteLeaderboardType.ForagingLog -> getAmount(
                leaderboardType.copy(mode = eliteLeaderboardMode),
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

        // Use the rank from the player if available, otherwise decrement
        val newRank = if (nextPlayer.rank != null && nextPlayer.rank > 0) {
            nextPlayer.rank
        } else {
            rankGoal ?: (currentRank - 1)
        }

        leaderboardPosMap?.set(leaderboardType, newRank)
        return lbData.nextPlayers.firstOrNull()
    }

    private fun loadLeaderboardIfAble(leaderboardType: EliteLeaderboardType): Int? {
        if (loadingLeaderboardMutex[leaderboardType::class]?.isLocked == true) return null

        val category = leaderboardType::class

        CoroutineSettings("load elite lb", timeout = 15.seconds).withIOContext().launchCoroutine {
            try {
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
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                apiUnavailable = true
                throw e
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
        // For unranked players (currentPos == Int.MAX_VALUE) the normal atRank calculation returns
        // null, which causes the Elite API to omit the "upcoming players" window entirely.
        // Re-using the upcomingRank from the previous API response gives the API an anchor so it
        // can return the bottom-of-the-leaderboard players on subsequent fetches.
        val previousUpcomingRank = lbData.apiData?.upcomingRank
        val atRank = getAtRank(currentPos, rankGoal, useRankGoal, previousUpcomingRank)

        val apiData = EliteDevApi.fetchLeaderboardPositions(
            profileId = profileId,
            lbType = leaderboardType,
            upcomingCount = upcomingPlayers,
            atRank = atRank,
            getLeaderboardConfig(leaderboardType).gamemode.get().apiMode,
        )
        // elite only updates player profiles once an hour, so assume it's wrong if it's the same as last fetch
        val shouldUpdateData = shouldUpdateData(leaderboardType, apiData)
        minAmount?.set(leaderboardType, apiData.minAmount)
        lbData.apiData = apiData
        lbData.lastUpdate = SimpleTimeMark.now()
        lbData.shouldRefresh = false
        apiError = false

        if (apiData.disabled) {
            apiUnavailable = true
            return leaderboardPosMap?.get(leaderboardType)
        }

        apiUnavailable = false

        if (apiData.rank <= 0) { // api returns -1 for unranked players
            lbData.isUnranked = true
            lbData.lastApiAmount = null
            if (leaderboardType is EliteLeaderboardType.ForagingLog)
                ForagingDebugLog.log("loadLBPos", leaderboardType.foragingLog, "UNRANKED path — apiData.rank=${apiData.rank} amount=${apiData.amount}")
            // correct wrong data
            leaderboardAmountMap?.remove(leaderboardType)
            leaderboardPosMap?.remove(leaderboardType)
            // For ForagingLog ALL_TIME the Elite API still returns the player's actual collection
            // count even when they are unranked — seed the local store with it so the
            // "until ranked" display shows the real remaining amount instead of session-only gains.
            // Guard with > 0 in case the API returns 0/invalid for this leaderboard type.
            // Guard with > current so a stale API response never overwrites fresher local data.
            if (leaderboardType is EliteLeaderboardType.ForagingLog &&
                leaderboardType.mode == EliteLeaderboardMode.ALL_TIME
            ) {
                val logType = leaderboardType.foragingLog
                if (logType != null && apiData.amount > 0) {
                    with(ForagingCollectionApi) {
                        val current = logType.getCollection()
                        if (apiData.amount.toLong() > current) {
                            logType.setCollectionCounter(apiData.amount.toLong())
                        }
                    }
                }
            }
            // For ForagingLog MONTHLY, initialise the session base to 0 instead of leaving it
            // null.  onForagingCollectionAdd uses ?: return, so without this the monthly amount
            // can never accumulate while the player is unranked.  Starting from 0 lets real-time
            // gains add up correctly from the moment the API confirms the player is unranked.
            if (leaderboardType is EliteLeaderboardType.ForagingLog &&
                leaderboardType.mode == EliteLeaderboardMode.MONTHLY
            ) {
                leaderboardAmountMap?.set(leaderboardType, 0.0)
            }
            if (!useRankGoal) {
                // Still populate nextPlayers so the overtake line can display
                // "X logs behind [LowestRankedPlayer]" instead of a bare "until ranked!" message.
                // handleUpcomingPlayers internally clears nextPlayers before re-populating.
                handleUpcomingPlayers(leaderboardType, apiData, false)
                lbData.lastPlayer = null
            }

            return null
        }
        lbData.isUnranked = false
        if (shouldUpdateData) handleDiff(leaderboardType, apiData)
        handleUpcomingPlayers(leaderboardType, apiData, shouldUpdateData)
        // On the very first fetch (currentPos == Int.MAX_VALUE) the API call had no atRank
        // anchor, so it returns upcoming players based on its own heuristic (often the top-N
        // boundary, e.g. rank 10 000) rather than the players adjacent to the player's actual
        // rank.  When no rank goal is active this means the display incorrectly shows e.g.
        // "2,177,026 behind ItsAqito [#10,000]" for a player at rank 61,286.  Schedule an
        // immediate re-fetch so the correct adjacent players are loaded after the throttle.
        if (currentPos == Int.MAX_VALUE && !useRankGoal && apiData.rank > 0) {
            lbData.shouldRefresh = true
        }
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

    private fun getAtRank(currentPos: Int, rankGoal: Int?, useRankGoal: Boolean, previousUpcomingRank: Int?): Int? = when {
        useRankGoal -> minOf((rankGoal ?: 0) + 1, currentPos)
        currentPos != Int.MAX_VALUE -> currentPos
        // For unranked players, no positional anchor is available.
        // Re-use the upcomingRank that the previous API response reported so the Elite API can
        // anchor the "upcoming players" window at the bottom of the leaderboard.  On first load
        // this will still be null, but the second fetch will have a valid anchor and will return
        // the last-ranked player(s), allowing the display to show "X behind [PlayerName]".
        else -> previousUpcomingRank?.takeIf { it > 0 }
    }

    // only update data if api data has changed since last request
    private fun shouldUpdateData(leaderboardType: EliteLeaderboardType, apiData: EliteLeaderboard): Boolean {
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        val lastApiAmount = lbData.lastApiAmount ?: return true
        return apiData.amount > lastApiAmount
    }

    private fun handleDiff(leaderboardType: EliteLeaderboardType, apiData: EliteLeaderboard) {
        if (apiData.rank == -1) return // no lb rank means amount is invalid
        when (leaderboardType) {
            is EliteLeaderboardType.Weight -> handleWeightDiff(leaderboardType, apiData)
            is EliteLeaderboardType.Crop -> handleCollectionDiff(leaderboardType, apiData)
            is EliteLeaderboardType.Pest -> handlePestDiff(leaderboardType, apiData)
            is EliteLeaderboardType.ForagingLog -> handleForagingDiff(leaderboardType, apiData)
        }
        eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }.apply {
            lastApiAmount = apiData.amount
            passedPlayers.clear()
        }
    }

    private fun handleWeightDiff(
        leaderboardType: EliteLeaderboardType,
        apiData: EliteLeaderboard,
    ) {
        setWeight(leaderboardType.mode, apiData.amount)
    }

    private fun handleCollectionDiff(
        leaderboardType: EliteLeaderboardType,
        apiData: EliteLeaderboard,
    ) {
        val crop = leaderboardType.crop ?: return
        when (leaderboardType.mode) {
            EliteLeaderboardMode.ALL_TIME -> crop.setCollectionCounter(apiData.amount.toLong())
            EliteLeaderboardMode.MONTHLY -> leaderboardAmountMap?.set(leaderboardType, apiData.amount)
        }
    }

    private fun handlePestDiff(
        leaderboardType: EliteLeaderboardType,
        apiData: EliteLeaderboard,
    ) {
        leaderboardAmountMap?.set(leaderboardType, apiData.amount)
    }

    private fun handleForagingDiff(
        leaderboardType: EliteLeaderboardType,
        apiData: EliteLeaderboard,
    ) {
        val logType = leaderboardType.foragingLog
        if (logType == null) {
            leaderboardAmountMap?.set(leaderboardType, apiData.amount)
            return
        }
        when (leaderboardType.mode) {
            EliteLeaderboardMode.ALL_TIME -> with(ForagingCollectionApi) {
                val current = logType.getCollection()
                if (apiData.amount.toLong() > current) {
                    logType.setCollectionCounter(apiData.amount.toLong())
                }
            }
            EliteLeaderboardMode.MONTHLY -> leaderboardAmountMap?.set(leaderboardType, apiData.amount)
        }
    }

    private fun handleUpcomingPlayers(
        leaderboardType: EliteLeaderboardType,
        apiData: EliteLeaderboard,
        updatedAmount: Boolean,
    ) {
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        val currentAmount = getAmount(leaderboardType) ?: apiData.amount
        val previousPlayer = apiData.previous?.firstOrNull()
        if (updatedAmount || lbData.lastPlayer == null) {
            lbData.lastPlayer = previousPlayer
        } else if (previousPlayer != null && currentAmount >= previousPlayer.amount) {
            lbData.lastPlayer = previousPlayer
        }
        lbData.nextPlayers.clear()
        apiData.upcomingPlayers.forEach {
            if (apiData.rank != 1 && it.amount > currentAmount) lbData.nextPlayers.add(it)
        }
    }

    fun getRankGoal(leaderboardType: EliteLeaderboardType): Int? {
        val lbData = eliteLeaderboardData.getOrPut(leaderboardType) { EliteLeaderboardData() }
        val goal = getRankGoalIfValid(leaderboardType)?.get()?.toIntOrNull() ?: return null

        val currentLeaderboardPos = leaderboardPosMap?.get(leaderboardType) ?: Int.MAX_VALUE

        if (goal !in 1..<currentLeaderboardPos) {
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
        apiUnavailable = false
        hasWarned = false
        fetchAttempts = 0
        lastFetchAttempt = SimpleTimeMark.farPast()
    }

    private fun farmingChatMessage(message: String) {
        farmingChatMessage(message.asComponent())
    }

    private fun farmingChatMessage(message: Component) {
        ChatUtils.chat {
            append(message)
            hover = componentBuilder {
                append("§eClick to open your Farming Weight")
                append("\n")
                append("§eprofile on §c${EliteDevApi.ELITE_DOMAIN}")
            }
            command = "/shfarmingprofile ${PlayerUtils.getName()}"
        }
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

    private const val BETTER_THAN_DEV_ACHIEVEMENT = "Better Than Dev Achievement"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Better than the devs".asComponent(),
            componentBuilder {
                append("Pass one of the")
                append(" SkyHanni ") {
                    withColor(TextHelper.chromaStyle)
                }
                append("contributors in the farming leaderboards")
            }
        )
        event.register(achievement, BETTER_THAN_DEV_ACHIEVEMENT)
    }
}
