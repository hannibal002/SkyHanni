package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.event.hoppity.HoppityEventSummaryConfig.HoppityStat
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.HoppityEventStatsStorage
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.HoppityEventStatsStorage.HoppityEventStats
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.HoppityEventStatsStorage.HoppityEventStats.RabbitData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.hoppity.RabbitFoundEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockTime.Companion.SKYBLOCK_DAY_MILLIS
import at.hannibal2.skyhanni.utils.SkyBlockTime.Companion.SKYBLOCK_HOUR_MILLIS
import at.hannibal2.skyhanni.utils.SkyblockSeason
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoppityEventSummary {
    private val config get() = SkyHanniMod.feature.event.hoppityEggs
    private val lineHeader = " ".repeat(4)

    private var firstInCf = 0L
    private var inCfNow = false

    private var lastLeaderboardInitWarning: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastLeaderboardFinWarning: SimpleTimeMark = SimpleTimeMark.farPast()
    private var needLeaderboardFinalization = false

    @HandleEvent
    fun onRabbitFound(event: RabbitFoundEvent) {
        if (!HoppityAPI.isHoppityEvent()) return
        val stats = ProfileStorageData.profileSpecific?.hoppityEvent?.stats ?: return

        stats.mealsFound.addOrPut(event.eggType, 1)
        val rarity = HoppityRabbitRarity.getByRabbit(event.rabbitName) ?: return
        val rarityMap = stats.rabbitsFound.getOrPut(rarity) { RabbitData() }
        if (event.duplicate) rarityMap.dupes++
        else rarityMap.uniques++
        if (event.chocGained > 0) stats.dupeChocolateGained += event.chocGained
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        checkEnded()
        if (!HoppityAPI.isHoppityEvent()) return
        checkLeaderboardInit()
        checkAddCfTime()
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        checkInit()
        checkEnded()
    }

    private fun checkInit() {
        val storage = ProfileStorageData.profileSpecific?.hoppityEvent ?: return
        val currentYear = SkyBlockTime.now().year
        storage.eventYear = storage.eventYear ?: run {
            if (SkyblockSeason.currentSeason == SkyblockSeason.SPRING) currentYear else currentYear + 1
        }
    }

    private fun checkLeaderboardInit() {
        if (!HoppityAPI.isHoppityEvent()) return
        if (!config.eventSummary.statDisplayList.contains(HoppityStat.LEADERBOARD_CHANGE)) return
        val stats = ProfileStorageData.profileSpecific?.hoppityEvent?.stats ?: return
        if (stats.initLbPos == null && lastLeaderboardInitWarning.passedSince() >= 60.seconds) {
            ChatUtils.chat("Open chocolate factory to set your initial Leaderboard position for Hoppity Event!")
            lastLeaderboardInitWarning = SimpleTimeMark.now()
        }
    }

    private fun updateCurrentLeaderboardPosition(position: Int?) {
        if (!HoppityAPI.isHoppityEvent() || position == null) return
        val stats = ProfileStorageData.profileSpecific?.hoppityEvent?.stats ?: return

        when {
            stats.initLbPos == null -> stats.initLbPos = position
            stats.finalLbPos == null && needLeaderboardFinalization -> {
                stats.finalLbPos = position
                needLeaderboardFinalization = false
            }
        }
    }

    private fun checkAddCfTime() {
        if (ChocolateFactoryAPI.inChocolateFactory && !this.inCfNow) {
            updateCurrentLeaderboardPosition(ChocolateFactoryAPI.leaderboardPosition)
            this.inCfNow = true
            firstInCf = SkyBlockTime.now().toMillis()
        } else if (!ChocolateFactoryAPI.inChocolateFactory && this.inCfNow) {
            val stats = ProfileStorageData.profileSpecific?.hoppityEvent?.stats ?: return
            stats.millisInCf += (SkyBlockTime.now().toMillis() - firstInCf)
            this.inCfNow = false
            firstInCf = 0
        }
    }

    private fun checkEnded() {
        val storage = ProfileStorageData.profileSpecific?.hoppityEvent ?: return
        if (storage.eventYear == null) return

        val currentYear = SkyBlockTime.now().year
        val currentSeason = SkyblockSeason.currentSeason
        val isSpring = currentSeason == SkyblockSeason.SPRING

        if (storage.eventYear!! < currentYear || (storage.eventYear!! == currentYear && !isSpring)) {
            handleEventEnd(storage)
        }
    }

    private fun handleEventEnd(storage: HoppityEventStatsStorage) {
        if (!config.eventSummary.enabled) return

        if (config.eventSummary.statDisplayList.contains(HoppityStat.LEADERBOARD_CHANGE) && storage.stats.finalLbPos == null) {
            needLeaderboardFinalization = true
            if (lastLeaderboardFinWarning.passedSince() >= 60.seconds) {
                ChatUtils.chat("Open chocolate factory to set your finalized Leaderboard position for Hoppity Event!")
                lastLeaderboardFinWarning = SimpleTimeMark.now()
            }
            return
        }

        sendStatsMessage(storage)
        ProfileStorageData.profileSpecific?.pastHoppityEventStats?.put(
            storage.eventYear!!, storage.stats,
        )
        ProfileStorageData.profileSpecific?.hoppityEvent = HoppityEventStatsStorage().apply {
            eventYear = null
            stats = HoppityEventStats()
        }
    }

    // First event was year 346 -> #1, 20th event was year 365, etc.
    private fun getHoppityEventNumber(skyblockYear: Int): Int = (skyblockYear - 345)

    fun addStrayCaught(rarity: HoppityRabbitRarity, chocGained: Long) {
        val stats = ProfileStorageData.profileSpecific?.hoppityEvent?.stats ?: return
        val rarityMap = stats.rabbitsFound.getOrPut(rarity) { RabbitData() }
        rarityMap.strays++
        stats.strayChocolateGained += chocGained
    }

    fun forceEventEnd() {
        ProfileStorageData.profileSpecific?.hoppityEvent?.eventYear ?: return
        val currentYear = SkyBlockTime.now().year
        ProfileStorageData.profileSpecific?.hoppityEvent!!.eventYear = currentYear - 1
    }

    private fun StringBuilder.appendHeadedLine(line: String) {
        appendLine("$lineHeader$line")
    }

    private fun StringBuilder.addExtraChocFormatLine(chocGained: Long) {
        if (chocGained <= 0) return
        appendHeadedLine(
            buildString {
                append(" §6+${chocGained.addSeparators()} Chocolate")
                if (SkyHanniMod.feature.inventory.chocolateFactory.showDuplicateTime) {
                    val timeFormatted = ChocolateFactoryAPI.timeUntilNeed(chocGained).format(maxUnits = 2)
                    append(" §7(§a+§b$timeFormatted§7)")
                }
            },
        )
    }

    private val summaryOperationList by lazy {
        buildMap<HoppityStat, (sb: StringBuilder, stats: HoppityEventStats) -> Unit> {
            put(HoppityStat.MEAL_EGGS_FOUND) { sb, stats ->
                stats.getEggsFoundFormat().forEach { foundFormat ->
                    sb.appendHeadedLine(foundFormat)
                }
            }

            put(HoppityStat.NEW_RABBITS) { sb, stats ->
                getRabbitsFormat(stats.rabbitsFound.mapValues { m -> m.value.uniques }, "Unique").forEach { newRabbitLine ->
                    sb.appendHeadedLine(newRabbitLine)
                }
            }

            put(HoppityStat.DUPLICATE_RABBITS) { sb, stats ->
                getRabbitsFormat(stats.rabbitsFound.mapValues { m -> m.value.dupes }, "Duplicate").forEach { dupeRabbitLine ->
                    sb.appendHeadedLine(dupeRabbitLine)
                }
                sb.addExtraChocFormatLine(stats.dupeChocolateGained)
            }

            put(HoppityStat.STRAY_RABBITS) { sb, stats ->
                getRabbitsFormat(stats.rabbitsFound.mapValues { m -> m.value.strays }, "Stray").forEach { strayRabbitLine ->
                    sb.appendHeadedLine(strayRabbitLine)
                }
                sb.addExtraChocFormatLine(stats.strayChocolateGained)
            }

            put(HoppityStat.LEADERBOARD_CHANGE) { sb, stats ->
                val initialPosition = stats.initLbPos ?: return@put
                val updatedPosition =
                    stats.finalLbPos.takeIf { lb -> lb != null } ?: ChocolateFactoryAPI.leaderboardPosition ?: return@put
                val leaderboardChange = initialPosition - updatedPosition
                if (leaderboardChange == 0) {
                    sb.appendHeadedLine("§7Leaderboard Change: §7§oNo change in position§r§7.")
                } else {
                    val changeFormat = "${if (leaderboardChange < 0) "§c" else "§a+"}$leaderboardChange"
                    sb.appendHeadedLine(
                        "§7Leaderboard Change: §b#$initialPosition §c-> §b#$updatedPosition §7($changeFormat spots§7)",
                    )
                }
            }

            put(HoppityStat.TIME_IN_CF) { sb, stats ->
                val timeFormat = stats.millisInCf.milliseconds.format(maxUnits = 2)
                sb.appendHeadedLine("§7You spent §b$timeFormat §7in the §6Chocolate Factory§7.")
            }

            put(HoppityStat.EMPTY) { sb, _ -> sb.appendLine() }
            put(HoppityStat.EMPTY_2) { sb, _ -> sb.appendLine() }
            put(HoppityStat.EMPTY_3) { sb, _ -> sb.appendLine() }
            put(HoppityStat.EMPTY_4) { sb, _ -> sb.appendLine() }
        }
    }

    fun sendStatsMessage(it: Array<String>) {
        val pastStats = ProfileStorageData.profileSpecific?.pastHoppityEventStats
        val statsYearList = pastStats?.keys.takeIf { it != null && it.size != 0 }?.toMutableList() ?: mutableListOf<Int>()
        val eventStorage = ProfileStorageData.profileSpecific?.hoppityEvent
        if (eventStorage?.eventYear != null && eventStorage.stats.rabbitsFound.any()) statsYearList.add(eventStorage.eventYear!!)

        val parsedInt: Int? = if (it.size == 1) it[0].toIntOrNull() else null

        if (parsedInt == null) {
            ChatUtils.chat("Stats are available for the following years:\n${statsYearList.joinToString("§e,") { "§b$it" }}")
        } else if (!statsYearList.contains(parsedInt)) {
            ChatUtils.chat(
                "Could not find stats for year §b$parsedInt§e. Stats are available for the following years:\n${
                    statsYearList.joinToString(
                        "§e,",
                    ) { "§b$it" }
                }",
            )
        } else if (parsedInt == eventStorage?.eventYear) {
            sendStatsMessage(eventStorage)
        } else {
            val historicalStats = pastStats?.get(parsedInt) ?: return
            sendStatsMessage(parsedInt, historicalStats)
        }
    }

    private fun sendStatsMessage(storage: HoppityEventStatsStorage) = sendStatsMessage(storage.eventYear, storage.stats)
    private fun sendStatsMessage(eventYear: Int?, stats: HoppityEventStats) {
        if (eventYear == null) return
        val summaryBuilder: StringBuilder = StringBuilder()
        summaryBuilder.appendLine("§d§l${"▬".repeat(64)}")

        // Header
        summaryBuilder.appendLine("${" ".repeat(26)}§d§lHoppity's Hunt #${getHoppityEventNumber(eventYear)} Stats")
        summaryBuilder.appendLine()

        // Various stats from config
        config.eventSummary.statDisplayList.forEach {
            summaryOperationList[it]?.invoke(summaryBuilder, stats)
        }

        // Footer
        summaryBuilder.appendLine("§d§l${"▬".repeat(64)}")
        ChatUtils.chat(summaryBuilder.toString(), prefix = false)
    }

    private fun HoppityEventStats.getEggsFoundFormat(): List<String> {
        val eggsFoundFormatList: MutableList<String> = buildList {
            mealsFound.filterKeys { it in HoppityEggType.resettingEntries }.sumAllValues().toInt().takeIf { it > 0 }?.let {
                add("§7You found §b$it§7/§a${getMealEggsSinceStart()} §6Chocolate Meal ${StringUtils.pluralize(it, "Egg")}§7.")
            }
            mealsFound[HoppityEggType.SIDE_DISH]?.let {
                add("§7You found §b$it §6§lSide Dish §r§6${StringUtils.pluralize(it, "Egg")}§7 §7in the §6Chocolate Factory§7.")
            }
            mealsFound[HoppityEggType.BOUGHT]?.let {
                add("§7You bought §b$it §f${StringUtils.pluralize(it, "Rabbit")} §7from §aHoppity§7.")
            }
        }.toMutableList()

        return if (eggsFoundFormatList.isEmpty()) listOf("§cNo Chocolate Eggs or Rabbits found during this event§7.")
        else eggsFoundFormatList
    }

    private fun getMealEggsSinceStart(): Int {
        if (!HoppityAPI.isHoppityEvent()) return 0

        val sbTimeNow = SkyBlockTime.now()
        val milliDifference = sbTimeNow.toMillis() - SkyBlockTime.fromSbYear(sbTimeNow.year).toMillis()

        // Calculate total eggs from complete days and incomplete day periods
        var spawnedMealsEggs = (milliDifference / SKYBLOCK_DAY_MILLIS).toInt() * 3

        // Add eggs for the current day based on time of day
        spawnedMealsEggs += when {
            milliDifference % SKYBLOCK_DAY_MILLIS >= SKYBLOCK_HOUR_MILLIS * 21 -> 3 // Dinner egg, 9 PM
            milliDifference % SKYBLOCK_DAY_MILLIS >= SKYBLOCK_HOUR_MILLIS * 14 -> 2 // Lunch egg, 2 PM
            milliDifference % SKYBLOCK_DAY_MILLIS >= SKYBLOCK_HOUR_MILLIS * 7 -> 1 // Breakfast egg, 7 AM
            else -> 0
        }

        return spawnedMealsEggs
    }

    private fun getRabbitsFormat(rarityMap: Map<HoppityRabbitRarity, Int>, name: String): List<String> {
        val rabbitsSum = rarityMap.values.sum()
        if (rabbitsSum == 0) return emptyList()

        return mutableListOf(
            "§7$name Rabbits: §f$rabbitsSum",
            HoppityRabbitRarity.entries.joinToString(" §7-") {
                " ${it.colorCode}${rarityMap[it] ?: 0}"
            },
        )
    }
}
