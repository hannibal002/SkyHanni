package at.hannibal2.skyhanni.features.event.hoppity.summary

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.event.hoppity.summary.HoppityEventSummaryConfig.HoppityStat
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.HoppityEventStats
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.HoppityEventStats.Companion.LeaderboardPosition
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.HoppityEventStats.Companion.RabbitData
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.hoppity.RabbitFoundEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi.getEventEndMark
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi.getHoppityEventNumber
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi.isAlternateDay
import at.hannibal2.skyhanni.features.event.hoppity.HoppityCollectionStats
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggType
import at.hannibal2.skyhanni.features.event.hoppity.summary.HoppityEventSummary.StatString
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi.partyModeReplace
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockTime.Companion.SKYBLOCK_DAY_MILLIS
import at.hannibal2.skyhanni.utils.SkyblockSeason
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumByKey
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

typealias MappedStatStrings = Map<HoppityStat, MutableList<StatString>>

@SkyHanniModule
@Suppress("LargeClass")
object HoppityEventSummary {
    /**
     * REGEX-TEST: §d§lHOPPITY'S HUNT §r§7You found §r§cRabbit the Fish§r§7!
     */
    private val rabbitTheFishPattern by CFApi.patternGroup.pattern(
        "rabbit.thefish",
        "(?:§.)*HOPPITY'S HUNT (?:§.)*You found (?:§.)*Rabbit the Fish(?:§.)*!.*",
    )

    private const val LINE_HEADER = "    "
    private val SEPARATOR = "§d§l${"▬".repeat(64)}"
    private val config get() = SkyHanniMod.feature.event.hoppityEggs
    private val statDisplayList get() = config.eventSummary.statDisplayList.get()
    private val storage get() = ProfileStorageData.profileSpecific
    private val updateCfConfig get() = config.eventSummary.cfReminder
    private val currentSbYear get() = SkyBlockTime.now().year
    private val yearSpawnCache: TimeLimitedCache<Int, Map<HoppityEggType, Int>> = TimeLimitedCache(30.seconds)
    private val yearCache: TimeLimitedSet<Int> = TimeLimitedSet(5.seconds)

    private var lastAddedCfMillis: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastSentCfUpdateMessage: SimpleTimeMark = SimpleTimeMark.farPast()
    private var currentEventEndMark: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastSnapshotServer: String? = null
    var statYear: Int = currentSbYear

    private fun MutableList<StatString>.chromafyHoppityStats(): MutableList<StatString> = map {
        if (CFApi.config.partyMode.get()) it.copy(string = it.string.partyModeReplace())
        else it
    }.toMutableList()

    data class StatString(var string: String, val headed: Boolean = true)

    private fun MutableList<StatString>.addStr(string: String, headed: Boolean = true) = this.add(StatString(string, headed))

    private fun MutableList<StatString>.addEmptyLine() = this.addStr("", false)

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresethoppityeventstats") {
            description = "Reset Hoppity Event stats for all years."
            category = CommandCategory.USERS_RESET
            callback { handleResetRequest(it) }
        }
    }

    @HandleEvent
    fun onEggSpawned() {
        yearSpawnCache.remove(currentSbYear)
    }

    @HandleEvent
    fun onRabbitFound(event: RabbitFoundEvent) {
        yearSpawnCache.remove(currentSbYear)
        val stats = getYearStats() ?: return
        if (!HoppityApi.isHoppityEvent()) {
            DelayedRun.runDelayed(5.seconds) {
                stats.typeCountsSince = HoppityCollectionStats.getTypeCountSnapshot()
            }
            return
        }

        stats.mealsFound.addOrPut(event.eggType, 1)
        val rarity = HoppityApi.rarityByRabbit(event.rabbitName) ?: return
        val rarityMap = stats.rabbitsFound.getOrPut(rarity) { RabbitData() }
        if (event.duplicate) rarityMap.dupes++
        else rarityMap.uniques++
        if (event.chocGained > 0) stats.dupeChocolateGained += event.chocGained

        // Make sure we account for event priority, since HoppityCollectionStats has a statically set lower priority
        DelayedRun.runDelayed(5.seconds) {
            stats.typeCountSnapshot = HoppityCollectionStats.getTypeCountSnapshot()
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!HoppityApi.isHoppityEvent()) return
        val stats = getYearStats() ?: return

        if (rabbitTheFishPattern.matches(event.message)) {
            stats.rabbitTheFishFinds++
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(64, "event.hoppity.preventMissingFish", "event.hoppity.preventMissingRabbitTheFish")
        event.move(65, "hoppityStatLiveDisplayToggled", "hoppityStatLiveDisplayToggledOff")

        event.transform(79, "#profile.hoppityEventStats") { element ->
            element.asJsonObject.apply {
                val empty = ConfigManager.gson.toJsonTree(RabbitData.EMPTY)
                entrySet().forEach { (_, stats) ->
                    stats.asJsonObject.add("typeCountSnapshot", empty)
                    stats.asJsonObject.add("typeCountsSince", empty)
                }
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        checkStatsTypeCountInit()
        checkLbUpdateWarning()
        checkEnded()
        if (!HoppityApi.isHoppityEvent()) return
        checkAddCfTime()
    }

    @HandleEvent
    fun onProfileJoin() {
        lastSnapshotServer = null
        checkEnded()
    }

    private fun handleResetRequest(args: Array<String>) {
        if (args.any { it.equals("confirm", ignoreCase = true) }) {
            resetStats()
            return
        }
        ChatUtils.clickableChat(
            "§c§lWARNING! §r§7This will reset §call §7Hoppity Event stats for §call §7years. " +
                "Click here or type §c/shresethoppityeventstats confirm §7to confirm.",
            onClick = HoppityEventSummary::resetStats,
        )
    }

    private fun resetStats() {
        storage?.let {
            it.hoppityEventStats.clear()
            ChatUtils.chat("Hoppity Event stats have been reset.")
        } ?: ErrorManager.skyHanniError("Could not reset Hoppity Event stats.")
    }

    private fun checkStatsTypeCountInit() {
        val stats = getYearStats() ?: return
        for (i in 0..2) {
            if (stats.typeCountSnapshot?.getByIndex(i) != 0) return
        }
        stats.typeCountSnapshot = HoppityCollectionStats.getTypeCountSnapshot()
    }

    private fun checkLbUpdateWarning() {
        if (!HoppityApi.isHoppityEvent() || !updateCfConfig.enabled) return

        // Only run if the user has leaderboard stats enabled
        if (!statDisplayList.contains(HoppityStat.LEADERBOARD_CHANGE)) return

        // If we're only showing the live display during the last {X} hours of the hunt,
        // check if we're in that time frame
        val showLastXHours = updateCfConfig.showForLastXHours.takeIf { it > 0 } ?: return

        // Initialize the current event end mark if it hasn't been set yet
        if (currentEventEndMark.isFarPast()) currentEventEndMark = getEventEndMark(currentSbYear)
        if (showLastXHours < 30 && currentEventEndMark.timeUntil() >= showLastXHours.hours) return

        // If it's been less than {config} minutes since the last warning message, don't send another
        lastSentCfUpdateMessage.takeIfInitialized()?.let {
            if (it.passedSince() < updateCfConfig.reminderInterval.minutes) return
        }

        // If it's been more than {config} since the last leaderboard update, send a message
        val stats = getYearStats() ?: return
        val lastLbUpdate = stats.lastLbUpdate.takeIfInitialized() ?: SimpleTimeMark.farPast()
        if (lastLbUpdate.passedSince() >= updateCfConfig.reminderInterval.minutes) {
            lastSentCfUpdateMessage = SimpleTimeMark.now()
            ChatUtils.chat(
                "§6§lReminder! §r§eSwitch to a new server and run §6/cf §eto " +
                    "update your leaderboard position in Hoppity Event stats.",
            )
        }
    }

    private fun getUnsummarizedYearStats(): Map<Int, HoppityEventStats> =
        storage?.hoppityEventStats?.filterValues { !it.summarized }.orEmpty()

    fun getYearStats(year: Int = currentSbYear): HoppityEventStats? =
        if (year == Int.MAX_VALUE) getAllTimeStats()
        else storage?.hoppityEventStats?.getOrPut(year) { HoppityEventStats(year) }

    private fun getAllTimeStats(): HoppityEventStats {
        val storageYears = storage?.hoppityEventStats?.keys ?: return HoppityEventStats()
        val allTimeStats = HoppityEventStats(storageYears)
        val statsStorage = storage?.hoppityEventStats ?: return allTimeStats
        statsStorage.values.forEach {
            allTimeStats += it
        }
        allTimeStats.initialLeaderboardPosition = statsStorage.values.firstOrNull {
            it.initialLeaderboardPosition.position != -1
        }?.initialLeaderboardPosition ?: LeaderboardPosition(-1, -1.0)
        allTimeStats.finalLeaderboardPosition = statsStorage.values.reversed().firstOrNull {
            it.finalLeaderboardPosition.position != -1
        }?.finalLeaderboardPosition ?: LeaderboardPosition(-1, -1.0)
        return allTimeStats
    }

    private fun checkAddCfTime() {
        if (!CFApi.inChocolateFactory) {
            lastAddedCfMillis = SimpleTimeMark.farPast()
            return
        }
        val stats = getYearStats() ?: return
        lastAddedCfMillis.takeIfInitialized()?.let {
            stats.millisInCf += it.passedSince()
        }
        lastAddedCfMillis = SimpleTimeMark.now()
    }

    private fun checkEnded() {
        if (!config.eventSummary.enabled) return
        val currentSeason = SkyblockSeason.currentSeason ?: return
        if (currentSbYear - 1 in yearCache) return
        yearCache.add(currentSbYear)

        getUnsummarizedYearStats().filter {
            it.key < currentSbYear || (it.key == currentSbYear && currentSeason > SkyblockSeason.SPRING)
        }.forEach { (year, stats) ->
            storage?.hoppityEventStats?.get(year)?.let {
                // Only send the message if we're going to be able to set the stats as summarized
                sendStatsMessage(stats, year)
                it.summarized = true
            }
        }
    }

    private fun inSameServer(): Boolean {
        val serverId = HypixelData.serverId ?: return false
        val lastServer = lastSnapshotServer
        lastSnapshotServer = serverId
        return serverId == lastServer
    }

    fun updateCfPosition(position: Int?, percentile: Double?) {
        if (!HoppityApi.isHoppityEvent() || inSameServer() || position == null || percentile == null) return
        val stats = getYearStats() ?: return
        val snapshot = LeaderboardPosition(position, percentile)
        stats.initialLeaderboardPosition = stats.initialLeaderboardPosition.takeIf { it.position != -1 } ?: snapshot
        stats.finalLeaderboardPosition = snapshot
        stats.lastLbUpdate = SimpleTimeMark.now()
    }

    fun addStrayCaught(rarity: LorenzRarity, chocGained: Long) {
        if (!HoppityApi.isHoppityEvent()) return
        val stats = getYearStats() ?: return
        val rarityMap = stats.rabbitsFound.getOrPut(rarity) { RabbitData() }
        rarityMap.strays++
        stats.strayChocolateGained += chocGained
    }

    private fun StringBuilder.appendHeadedLine(line: String) {
        appendLine("$LINE_HEADER$line")
    }

    private fun MutableList<StatString>.addExtraChocFormatLine(chocGained: Long) {
        if (chocGained <= 0) return
        val chocFormatLine = buildString {
            append(" §6+${chocGained.addSeparators()} Chocolate")
            if (SkyHanniMod.feature.inventory.chocolateFactory.showDuplicateTime) {
                val timeFormatted = CFApi.timeUntilNeed(chocGained).format(maxUnits = 2)
                append(" §7(§a+§b$timeFormatted§7)")
            }
        }
        add(StatString(chocFormatLine))
    }

    private fun getPreviousStats(year: Int): HoppityEventStats? =
        storage?.hoppityEventStats?.get(year - 1)

    private fun HoppityEventStats.getMilestoneCount(): Int =
        (mealsFound[HoppityEggType.CHOCOLATE_FACTORY_MILESTONE] ?: 0) +
            (mealsFound[HoppityEggType.CHOCOLATE_SHOP_MILESTONE] ?: 0)

    private fun HoppityEventStats.getBoughtCount(): Int =
        (mealsFound[HoppityEggType.BOUGHT] ?: 0) + (mealsFound[HoppityEggType.BOUGHT_ABIPHONE] ?: 0)

    fun HoppityEventStats.getMealEggCounts(): Map<HoppityEggType, Int> =
        mealsFound.filterKeys { it in HoppityEggType.resettingEntries }

    private fun formatRabbits(
        stats: HoppityEventStats,
        transform: (Map.Entry<LorenzRarity, RabbitData>) -> Int,
        name: String,
        year: Int,
        index: Int,
        statList: MutableList<StatString>,
    ) = getRabbitsFormat(
        rarityMap = stats.rabbitsFound.mapValues(transform),
        name = name,
        countTriple = stats.getPairTriple(year, index),
    ) { statList.addStr(it) }

    private val summaryOperationList by lazy {
        buildMap<HoppityStat, (statList: MutableList<StatString>, stats: HoppityEventStats, year: Int) -> Unit> {
            put(HoppityStat.MEAL_EGGS_FOUND) { statList, stats, year ->
                val totalMealEggs = stats.getMealEggCounts().sumAllValues().toInt().takeIf { it > 0 } ?: return@put
                val spawnedMealEggs = stats.getSpawnedEggCountsWithInfPossible(year).sumAllValues()
                val eggFormat = StringUtils.pluralize(totalMealEggs, "Egg")
                val amount = "$totalMealEggs§7/§a${spawnedMealEggs.addSeparators()}"
                statList.addStr("§7You found §b$amount §6Chocolate Meal $eggFormat§7.")
            }

            put(HoppityStat.HITMAN_EGGS) { statList, stats, year ->
                // We only want to show events after hitman was added (Hunt #41)
                getHoppityEventNumber(year).takeIf { it > 41 } ?: return@put
                val hitmanCount = stats.mealsFound[HoppityEggType.HITMAN]?.takeIf { it > 0 } ?: return@put

                val spawnedMealEggs = stats.getSpawnedEggCountsWithInfPossible(year)
                val collectedEggs = stats.getMealEggCounts()
                val missedMealEggs = (spawnedMealEggs).map { (type, spawnedCount) ->
                    val collectedOfType = collectedEggs[type] ?: 0
                    type to (spawnedCount - collectedOfType)
                }.toMap().sumAllValues().toInt()

                val eggFormat = StringUtils.pluralize(missedMealEggs, "Egg")
                val divisorFormat = "§b$hitmanCount§7/§a$missedMealEggs"
                statList.addStr("§7You recovered $divisorFormat §7missed §6Meal $eggFormat §7from §cRabbit Hitman§7.")
            }

            put(HoppityStat.HOPPITY_RABBITS_BOUGHT) { statList, stats, _ ->
                val boughtCount = stats.getBoughtCount().takeIf { it > 0 } ?: return@put
                val rabbitFormat = StringUtils.pluralize(boughtCount, "Rabbit")
                statList.addStr("§7You bought §b$boughtCount §f$rabbitFormat §7from §aHoppity§7.")
            }

            put(HoppityStat.SIDE_DISH_EGGS) { statList, stats, _ ->
                val sideDishCount = stats.mealsFound[HoppityEggType.SIDE_DISH]?.takeIf { it > 0 } ?: return@put
                val eggFormat = StringUtils.pluralize(sideDishCount, "Egg")
                statList.addStr("§7You found §b$sideDishCount §6§lSide Dish $eggFormat §r§7in the §6Chocolate Factory§7.")
            }

            put(HoppityStat.MILESTONE_RABBITS) { statList, stats, _ ->
                val milestoneCount = stats.getMilestoneCount().takeIf { it > 0 } ?: return@put
                val rabbitFormat = StringUtils.pluralize(milestoneCount, "Rabbit")
                statList.addStr("§7You claimed §b$milestoneCount §6§lMilestone $rabbitFormat§7.")
            }

            put(HoppityStat.NEW_RABBITS) { statList, stats, year ->
                formatRabbits(stats, { it.value.uniques }, "Unique", year, index = 0, statList)
            }

            put(HoppityStat.DUPLICATE_RABBITS) { statList, stats, year ->
                formatRabbits(stats, { it.value.dupes }, "Duplicate", year, index = 1, statList)
                statList.addExtraChocFormatLine(stats.dupeChocolateGained)
            }

            put(HoppityStat.STRAY_RABBITS) { statList, stats, year ->
                formatRabbits(stats, { it.value.strays }, "Stray", year, index = 2, statList)
                statList.addExtraChocFormatLine(stats.strayChocolateGained)
            }

            put(HoppityStat.TIME_IN_CF) { statList, stats, _ ->
                val timeInCf = stats.millisInCf.takeIf { it > Duration.ZERO } ?: return@put
                val timeFormat = timeInCf.format(maxUnits = 2)
                statList.addStr("§7You spent §b$timeFormat §7in the §6Chocolate Factory§7.")
            }

            put(HoppityStat.RABBIT_THE_FISH_FINDS) { statList, stats, _ ->
                val rabbitTheFishFinds = stats.rabbitTheFishFinds.takeIf { it > 0 } ?: return@put
                val timesFormat = StringUtils.pluralize(rabbitTheFishFinds, "time")
                statList.addStr("§7You found §cRabbit the Fish §7in Meal Eggs §b$rabbitTheFishFinds §7$timesFormat.")
            }

            put(HoppityStat.LEADERBOARD_CHANGE) { statList, stats, _ ->
                val initial = stats.initialLeaderboardPosition
                val final = stats.finalLeaderboardPosition
                if (
                    initial.position == -1 || final.position == -1 ||
                    initial.percentile == -1.0 || final.percentile == -1.0 ||
                    initial.position == final.position
                ) return@put
                getFullLeaderboardMessage(initial, final).forEach {
                    statList.addStr(it)
                }
            }

            put(HoppityStat.EMPTY_1) { sl, _, _ -> sl.addEmptyLine() }
            put(HoppityStat.EMPTY_2) { sl, _, _ -> sl.addEmptyLine() }
            put(HoppityStat.EMPTY_3) { sl, _, _ -> sl.addEmptyLine() }
            put(HoppityStat.EMPTY_4) { sl, _, _ -> sl.addEmptyLine() }
        }
    }

    private fun getFullLeaderboardMessage(initial: LeaderboardPosition, final: LeaderboardPosition) = buildList {
        add("§7Leaderboard: ${getPrimaryLbString(initial, final)}")
        add(getSecondaryLbLine(initial, final))
    }

    private fun getPrimaryLbString(initial: LeaderboardPosition, final: LeaderboardPosition): String {
        val iPo = initial.position
        val fPo = final.position
        return "§b#${iPo.addSeparators()} §c-> §b#${fPo.addSeparators()}"
    }

    private fun getSecondaryLbLine(initial: LeaderboardPosition, final: LeaderboardPosition): String {
        val initPosition = initial.position
        val finalPosition = final.position
        val diffPosition = finalPosition - initPosition
        val initialPercentile = initial.percentile
        val finalPercentile = final.percentile
        val diffPercentile = finalPercentile - initialPercentile
        val preambleFormat = if (initPosition > finalPosition) "§a+" else "§c"

        return buildString {
            append(" §7($preambleFormat${(-1 * diffPosition).addSeparators()} ${StringUtils.pluralize(diffPosition, "spot")}§7)")
            if (diffPercentile != 0.0) append(" §7Top §a$initialPercentile% §c-> §7Top §a$finalPercentile%")
            else append(" §7Top §a$initialPercentile%")
        }
    }

    private val EMPTY_STATS = setOf(
        HoppityStat.EMPTY_1, HoppityStat.EMPTY_2,
        HoppityStat.EMPTY_3, HoppityStat.EMPTY_4
    )

    fun getMappedStatStrings(stats: HoppityEventStats, eventYear: Int): MappedStatStrings = statDisplayList.mapNotNull { stat ->
        val operator = summaryOperationList[stat] ?: return@mapNotNull null
        val operatedList: MutableList<StatString> = mutableListOf()
        operator.invoke(operatedList, stats, eventYear)

        val listEmpty = operatedList.isEmpty() || operatedList.all { it.string.removeColor().trim().isBlank() }
        if (listEmpty && stat !in EMPTY_STATS) return@mapNotNull null

        stat to operatedList
    }.toMap()

    fun MappedStatStrings.dropConsecutiveEmpties(): MappedStatStrings =
        entries.fold(mutableMapOf()) { acc, (stat, list) ->
            val nowEmpty = stat in EMPTY_STATS
            val lastEmpty = acc.keys.lastOrNull()?.let { it in EMPTY_STATS } ?: true

            if (!lastEmpty || !nowEmpty) acc[stat] = list
            acc
        }

    fun buildEmptyFallback(isCurrentEvent: Boolean): MutableList<StatString> {
        val timeFmt = if (isCurrentEvent) "§c§l§oRIGHT NOW§c§o" else "in the future"
        return mutableListOf(
            StatString("", false),
            StatString("§c§lNothing to show!"),
            StatString("§c§oFind some eggs $timeFmt!")
        )
    }

    private fun getStatsStrings(stats: HoppityEventStats, eventYear: Int?): MutableList<StatString> {
        if (eventYear == null) return mutableListOf()
        val statList = getMappedStatStrings(stats, eventYear)
            .dropConsecutiveEmpties()
            .values.flatten()
            .toMutableList()

        val finalStatList = if (statList.isEmpty() || statList.all { it.string.isBlank() }) {
            buildEmptyFallback(
                isCurrentEvent = HoppityApi.isHoppityEvent() && eventYear == currentSbYear
            ).toMutableList()
        } else statList

        return finalStatList.chromafyHoppityStats()
    }

    private fun sendStatsMessage(stats: HoppityEventStats, eventYear: Int?) {
        if (eventYear == null) return

        val statsString = buildString {
            getStatsStrings(stats, eventYear).forEach {
                if (it.headed) appendHeadedLine(it.string)
                else appendLine(it.string)
            }
        }

        val summary = buildString {
            appendLine(SEPARATOR)

            // Header
            val eventNumber = getHoppityEventNumber(eventYear)
            appendLine("${" ".repeat(26)}§d§lHoppity's Hunt #$eventNumber Stats")
            appendLine()

            // Append stats
            append(statsString)

            append(SEPARATOR)
        }

        ChatUtils.chat(summary, prefix = false)
    }

    fun HoppityEventStats.getSpawnedEggCountsWithInfPossible(year: Int): Map<HoppityEggType, Int> = when {
        (year == Int.MAX_VALUE) -> {
            containingYears.mapNotNull { containingYear ->
                if (containingYear == year) return@mapNotNull null
                getSpawnedEggCounts(containingYear)
            }.sumByKey().map {
                it.key to it.value.toInt()
            }.toMap()
        }
        else -> getSpawnedEggCounts(year)
    }

    fun getSpawnedEggCounts(year: Int): Map<HoppityEggType, Int> = when {
        (year in yearSpawnCache.keys) -> yearSpawnCache[year].orEmpty()
        (year > SkyBlockTime.now().year) -> mapOf()
        // Hoppity Event #41 was the first event with the new egg types
        (getHoppityEventNumber(year) < 41) -> mapOf(
            HoppityEggType.BREAKFAST to 93,
            HoppityEggType.LUNCH to 93,
            HoppityEggType.DINNER to 93,
        )
        // Events end with 47 of the non-alt day eggs, and 46 of the alt day eggs having spawned
        (year < SkyBlockTime.now().year) -> mapOf(
            HoppityEggType.BREAKFAST to 47,
            HoppityEggType.LUNCH to 47,
            HoppityEggType.DINNER to 47,

            HoppityEggType.BRUNCH to 46,
            HoppityEggType.DEJEUNER to 46,
            HoppityEggType.SUPPER to 46,
        )
        else -> {
            val milliDifference = SkyBlockTime.now().toMillis() - SkyBlockTime.fromSBYear(year).toMillis()
            val pastEvent = milliDifference > SkyBlockTime.SKYBLOCK_SEASON_MILLIS
            val fullDays = (milliDifference / SKYBLOCK_DAY_MILLIS).toInt()
            val remainderDays = fullDays % 2
            val now = SkyBlockTime.now()
            val isAltDayToday = now.isAlternateDay()
            val isAltDayRemainder = remainderDays == 1 && !isAltDayToday

            HoppityEggType.resettingEntries.associateWith { eggType ->
                var count = fullDays / 2

                // +1 if that extra full day spawned this egg
                if (remainderDays == 1 && isAltDayRemainder == eggType.altDay) count += 1
                // +1 if _today_ has already passed this egg’s reset time
                if (!pastEvent && isAltDayToday == eggType.altDay && now.hour >= eggType.resetsAt) count += 1

                count
            }
        }
    }.also { yearSpawnCache[year] = it }

    private fun HoppityEventStats.getPairTriple(
        year: Int,
        index: Int,
    ): Triple<Int, Int, Int> = getPreviousStats(year)?.let {
        val currentValue = this.typeCountSnapshot?.getByIndex(index) ?: 0
        val previousValue = it.typeCountSnapshot?.getByIndex(index) ?: 0
        val sinceValue = ((it.typeCountsSince?.getByIndex(index) ?: previousValue) - previousValue)
        val validData = previousValue > 0 && previousValue != currentValue && sinceValue > 0
        Triple(
            if (validData) previousValue else 0,
            if (validData) currentValue else 0,
            if (validData) sinceValue else 0,
        )
    } ?: Triple(0, 0, 0)

    fun getRabbitsFormat(
        rarityMap: Map<LorenzRarity, Int>,
        name: String,
        countTriple: Triple<Int, Int, Int> = Triple(0, 0, 0),
        action: (String) -> Unit,
    ) {
        val (prevCount, currCount, sinceCount) = countTriple
        val rabbitsSum = rarityMap.values.sum()
        if (rabbitsSum == 0) return

        val sinceFormat = if (sinceCount > 0) " §8+$sinceCount§7" else ""
        val countFormat = if (config.eventSummary.showCountDiff && prevCount != 0 && currCount != 0) {
            " §7($prevCount$sinceFormat -> $currCount)"
        } else ""

        listOf(
            "§7$name Rabbits: §f${rabbitsSum.addSeparators()}$countFormat",
            HoppityApi.hoppityRarities.joinToString(" §7-") {
                " ${it.chatColorCode}${(rarityMap[it] ?: 0).addSeparators()}"
            },
        ).forEach(action)
    }
}
