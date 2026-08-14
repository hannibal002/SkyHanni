package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.garden.farming.FarmingContestEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object FarmingContestApi {

    private val patternGroup = RepoPattern.group("garden.farming.contest")

    /**
     * REGEX-TEST: Spring 31st, Year 494
     */
    private val timePattern by patternGroup.pattern(
        "time.colorless",
        "(?<month>.*) (?<day>.*)(?:rd|st|nd|th), Year (?<year>.*)",
    )

    /**
     * REGEX-TEST: Carrot Contest
     */
    private val cropPattern by patternGroup.pattern(
        "crop.colorless",
        "(?<crop>.*) Contest",
    )

    /**
     * WRAPPED-REGEX-TEST: " ○ Cocoa Beans 15m14s"
     */
    private val sidebarCropPattern by patternGroup.pattern(
        "sidebarcrop.colorless",
        "\\s*(?:○|${SkyblockStat.FARMING_FORTUNE.hypixelIcon}) (?<crop>.*?)(?: \\d+(?:[hms]\\d*)+)?",
    )

    /**
     * REGEX-TEST: Jacob's Contest
     */
    private val contestTitlePattern by patternGroup.pattern(
        "title.colorless",
        "Jacob's Contest",
    )

    /**
     * REGEX-TEST: Jacob's Contest: 17m left
     * REGEX-TEST: Jacob's Contest: 1m 36s left
     */
    private val contestTimeLeftPattern by patternGroup.pattern(
        "timeleft.colorless",
        "Jacob's Contest: \\d+(?:[hms] ?\\d*)* left",
    )

    /**
     * REGEX-TEST: Claim multiple farming contest
     */
    private val bulkClaimFarmingPattern by patternGroup.pattern(
        "bulkclaim.colorless",
        "Claim multiple farming contest",
    )

    /**
     * REGEX-TEST: (1/2) Your Contests
     * REGEX-TEST: (2/2) Your Contests
     * REGEX-TEST: Your Contests
     */
    val yourContestsPattern by patternGroup.pattern(
        "yourcontests",
        "(?:\\(\\d+/\\d+\\) )?Your Contests",
    )

    private val contests = mutableMapOf<Long, FarmingContest>()
    private var internalContest = false
    private var scoreboardLines = emptyList<String>()
    private var contestWidgetLines = emptyList<String>()
    private var contestActive = false
    val inContest
        get() = internalContest && IslandTypeTag.CONTESTS_SHOWN.isInIsland()
    val isContestActive
        get() = contestActive && IslandTypeTag.CONTESTS_SHOWN.isInIsland()
    var contestCrop: CropType? = null
    private var startTime = SimpleTimeMark.farPast()
    var inInventory = false
        private set

    init {
        ContestBracket.entries.forEach { it.bracketPattern }
    }

    @HandleEvent
    fun onSecondPassed() {
        if (internalContest && startTime.passedSince() > 20.minutes) {
            FarmingContestEvent(contestCrop!!, FarmingContestPhase.STOP).post()
            internalContest = false
        }
    }

    @HandleEvent(ScoreboardUpdateEvent::class, onlyOnSkyblock = true)
    private fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        scoreboardLines = event.new.map { it.removeColor() }
        @Suppress("IsInIslandEarlyReturn")
        if (GardenApi.inGarden()) checkActiveContest()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.JACOB_CONTEST)) return
        contestWidgetLines = event.cleanLines.map { it.trim() }
        contestActive = contestWidgetLines.any { contestTimeLeftPattern.matches(it) }
    }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Farming Contest")
        event.addData {
            add("contest island allowed: ${IslandTypeTag.CONTESTS_SHOWN.isInIsland()}")
            add("Jacob tab widget visible: ${TabWidget.JACOB_CONTEST.isActive}")
            add("contest active: $isContestActive")
            add("internal contest: $internalContest")
            add("contest crop: ${contestCrop?.cropName}")
            add("Jacob tab widget lines:")
            if (contestWidgetLines.isEmpty()) add("  none")
            contestWidgetLines.forEachIndexed { index, line -> add("  [$index] '$line'") }
        }
    }

    private fun checkActiveContest() {
        val currentCrop = readCurrentCrop()
        val currentContest = currentCrop != null

        if (inContest != currentContest) {
            if (currentContest) {
                FarmingContestEvent(currentCrop, FarmingContestPhase.START).post()
                startTime = SimpleTimeMark.now()
            } else {
                if (startTime.passedSince() > 2.minutes) {
                    FarmingContestEvent(contestCrop!!, FarmingContestPhase.STOP).post()
                }
            }
            internalContest = currentContest
        } else {
            if (currentCrop != contestCrop && currentCrop != null) {
                FarmingContestEvent(currentCrop, FarmingContestPhase.CHANGE).post()
                startTime = SimpleTimeMark.now()
            }
        }
        contestCrop = currentCrop
    }

    private fun readCurrentCrop(): CropType? {
        val line = scoreboardLines.nextAfter(after = { contestTitlePattern.matches(it) }) ?: return null
        return sidebarCropPattern.matchMatcher(line) {
            val cropName = group("crop")
            try {
                CropType.getByName(cropName)
            } catch (e: IllegalStateException) {
                ErrorManager.logErrorWithData(
                    e, "Farming contest read current crop failed",
                    "cropName" to cropName,
                    "line" to line,
                    "scoreboardLines" to scoreboardLines,
                )
                null
            }
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST, onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!yourContestsPattern.matches(event.inventoryName)) return
        if (inInventory) return
        val bulkClaimStack = event.inventoryItems[50] ?: return
        val firstLine = bulkClaimStack.getCleanLore().firstOrNull() ?: return
        if (!bulkClaimFarmingPattern.matches(firstLine)) return
        inInventory = true
    }

    @HandleEvent
    fun onInventoryClose() {
        inInventory = false
    }

    fun getSBDateFromItemName(text: String): List<String>? = timePattern.matchMatcher(text) {
        listOf(group("year"), group("month"), group("day"))
    }

    fun getSBTimeFor(text: String): Long? {
        val (year, month, day) = getSBDateFromItemName(text) ?: return null
        val monthNr = SkyBlockTime.getSBMonthByName(month)

        return SkyBlockTime(year.toInt(), monthNr, day.toInt()).toMillis()
    }

    fun addContest(time: Long, item: SafeItemStack) {
        contests.putIfAbsent(time, createContest(time, item))
    }

    private fun createContest(time: Long, item: SafeItemStack): FarmingContest {
        val lore = item.getCleanLore()

        val crop = cropPattern.firstMatcher(lore) {
            CropType.getByName(group("crop"))
        } ?: error("Crop not found in lore!")

        val brackets = buildMap {
            for (bracket in ContestBracket.entries) {
                val amount = bracket.bracketPattern.firstMatcher(lore) {
                    group("amount").formatInt()
                } ?: continue
                put(bracket, amount)
            }
        }

        return FarmingContest(time, crop, brackets)
    }

    fun getContestAtTime(time: Long) = contests[time]

    fun getContestsOfType(crop: CropType) = contests.values.filter { it.crop == crop }

    fun calculateAverages(crop: CropType): Pair<Int, Map<ContestBracket, Int>> {
        var amount = 0
        val crops = mutableMapOf<ContestBracket, Int>()
        val contests = mutableMapOf<ContestBracket, Int>()
        for (contest in getContestsOfType(crop).associateWith { it.time }.sortedDesc().keys) {
            amount++
            val brackets = contest.brackets
            for ((bracket, count) in brackets) {
                val old = crops.getOrDefault(bracket, 0)
                crops[bracket] = count + old
                contests.addOrPut(bracket, 1)
            }
            if (amount == 10) break
        }
        return Pair(amount, crops.mapValues { (bracket, counter) -> counter / contests[bracket]!! })
    }
}
