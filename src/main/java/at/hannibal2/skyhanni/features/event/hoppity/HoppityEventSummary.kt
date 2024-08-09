package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.event.hoppity.HoppityEventSummaryConfig.HoppityStat
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.HoppityEventStatsStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.hoppity.RabbitFoundEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
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

    private var needInitLeaderboard = false
    private var lastLeaderboardInitWarning: SimpleTimeMark = SimpleTimeMark.farPast()
    private var needFinalLeaderboard = false
    private var lastLeaderboardFinWarning: SimpleTimeMark = SimpleTimeMark.farPast()

    @HandleEvent
    fun onRabbitFound(event: RabbitFoundEvent) {
        if (!HoppityAPI.isHoppityEvent()) return
        val stats = ProfileStorageData.profileSpecific?.hoppityEventStats ?: return

        stats.mealType.addOrPut(event.eggType, 1)
        val rarity = HoppityRabbitRarity.getByRabbit(event.rabbitName) ?: return
        if (event.duplicate) stats.dupeRabbits.addOrPut(rarity, 1)
        else stats.newRabbits.addOrPut(rarity, 1)
        if (event.chocGained > 0) stats.chocolateGained += event.chocGained
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        checkLeaderboardInit()
        checkAddCfTime()
        checkEnded()
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        checkEnded()
    }

    // First event was year 346 -> #1, 20th event was year 365, etc.
    private fun getHoppityEventNumber(skyblockYear: Int): Int = (skyblockYear - 345)

    fun addStrayCaught(rarity: HoppityRabbitRarity, chocGained: Long) {
        val stats = ProfileStorageData.profileSpecific?.hoppityEventStats ?: return
        stats.strayRabbits.addOrPut(rarity, 1)
        stats.strayChocolateGained += chocGained
    }

    private fun updateCurrentLeaderboardPosition(position: Int?) {
        if (!HoppityAPI.isHoppityEvent()) return
        if (position == null) return
        val stats = ProfileStorageData.profileSpecific?.hoppityEventStats ?: return
        if (needInitLeaderboard) {
            stats.initLbPos = position
        } else if (needFinalLeaderboard) {
            stats.finalLbPos = position
        }
    }

    fun forceEventEnd() {
        ProfileStorageData.profileSpecific?.hoppityEventStats?.currentYear ?: return
        val currentYear = SkyBlockTime.now().year
        ProfileStorageData.profileSpecific?.hoppityEventStats!!.currentYear = currentYear - 1
    }

    private fun checkLeaderboardInit() {
        if (!HoppityAPI.isHoppityEvent()) return
        if (!config.eventSummary.statDisplayList.contains(HoppityStat.TIME_IN_CF)) return
        val stats = ProfileStorageData.profileSpecific?.hoppityEventStats ?: return
        if (stats.initLbPos == null) {
            needInitLeaderboard = true
            if (lastLeaderboardInitWarning.passedSince() >= 60.seconds) {
                ChatUtils.chat("Open chocolate factory to set your initial Leaderboard position for Hoppity Event!")
                lastLeaderboardInitWarning = SimpleTimeMark.now()
            }
        } else needInitLeaderboard = false
    }

    private fun checkAddCfTime() {
        if (ChocolateFactoryAPI.inChocolateFactory && !inCfNow) {
            updateCurrentLeaderboardPosition(ChocolateFactoryAPI.leaderboardPosition)
            inCfNow = true
            firstInCf = SkyBlockTime.now().toMillis()
        } else if (!ChocolateFactoryAPI.inChocolateFactory && inCfNow) {
            val stats = ProfileStorageData.profileSpecific?.hoppityEventStats ?: return
            stats.millisInCf += (SkyBlockTime.now().toMillis() - firstInCf)
            inCfNow = false
            firstInCf = 0
        }
    }

    private fun checkEnded() {
        if (!config.eventSummary.enabled) return
        val stats = ProfileStorageData.profileSpecific?.hoppityEventStats ?: return

        val currentYear = SkyBlockTime.now().year
        val currentSeason = SkyblockSeason.currentSeason

        if (stats == HoppityEventStatsStorage()) {
            ProfileStorageData.profileSpecific!!.hoppityEventStats!!.currentYear = currentYear
            return
        }

        val ended = stats.currentYear < currentYear || (stats.currentYear == currentYear && currentSeason != SkyblockSeason.SPRING)
        if (ended) {
            if (config.eventSummary.statDisplayList.contains(HoppityStat.TIME_IN_CF) && stats.finalLbPos == null) {
                needFinalLeaderboard = true
                if (lastLeaderboardFinWarning.passedSince() >= 60.seconds) {
                    ChatUtils.chat("Open chocolate factory to set your finalized Leaderboard position for Hoppity Event!")
                    lastLeaderboardFinWarning = SimpleTimeMark.now()
                }
                return
            } else needFinalLeaderboard = false
            sendSummaryMessage(SummaryType.CONCLUDED, stats)
            ProfileStorageData.profileSpecific?.pastHoppityEventStats?.put(
                stats.currentYear, stats
            )
            ProfileStorageData.profileSpecific?.hoppityEventStats = HoppityEventStatsStorage()
            ProfileStorageData.profileSpecific?.hoppityEventStats!!.currentYear = currentYear + 1
        }
    }

    enum class SummaryType(val displayName: String) {
        CONCLUDED("Concluded"),
        PROGRESS("Progress")
        ;

        override fun toString(): String = displayName
    }

    fun sendProgressMessage() {
        if (!HoppityAPI.isHoppityEvent()) {
            ChatUtils.chat("§eThis command only works while §d§lHoppity's Hunt §eis active.", prefix = false)
            return
        }
        val stats = ProfileStorageData.profileSpecific?.hoppityEventStats
            ?: ErrorManager.skyHanniError("Could not read stats for current Hoppity's Event")

        sendSummaryMessage(SummaryType.PROGRESS, stats)
    }

    private fun StringBuilder.appendHeadedLine(line: String) {
        appendLine("$lineHeader$line")
    }

    private fun StringBuilder.addExtraChocFormatLine(chocGained: Long) {
        if (chocGained <= 0) return
        var extraChocFormat = " §6+${chocGained.addSeparators()} Chocolate"
        if (SkyHanniMod.feature.inventory.chocolateFactory.showDuplicateTime) {
            val timeFormatted = ChocolateFactoryAPI.timeUntilNeed(chocGained).format(maxUnits = 2)
            extraChocFormat += " §7(§a+§b${timeFormatted}§7)"
        }
        appendHeadedLine(extraChocFormat)
    }

    private val summaryOperationList by lazy {
        buildMap<HoppityStat, (StringBuilder) -> Unit> {
            val stats = ProfileStorageData.profileSpecific?.hoppityEventStats
            if (stats != null) {
                put(HoppityStat.MEAL_EGGS_FOUND) {
                    stats.getEggsFoundFormat().forEach {
                        foundFormat -> it.appendHeadedLine(foundFormat)
                    }
                }

                put(HoppityStat.NEW_RABBITS) {
                    getRabbitsFormat(stats.newRabbits, "Unique", "§b").forEach {
                        newRabbitLine -> it.appendHeadedLine(newRabbitLine)
                    }
                }

                put(HoppityStat.DUPLICATE_RABBITS) {
                    getRabbitsFormat(stats.dupeRabbits, "Duplicate", "§c").forEach {
                        dupeRabbitLine -> it.appendHeadedLine(dupeRabbitLine)
                    }
                    it.addExtraChocFormatLine(stats.chocolateGained)
                }

                put(HoppityStat.STRAY_RABBITS) {
                    getRabbitsFormat(stats.strayRabbits, "Stray", "§f").forEach {
                        strayRabbitLine -> it.appendHeadedLine(strayRabbitLine)
                    }
                    it.addExtraChocFormatLine(stats.strayChocolateGained)
                }

                put(HoppityStat.LEADERBOARD_CHANGE) {
                    val initialPosition = stats.initLbPos ?: return@put
                    val updatedPosition = stats.finalLbPos.takeIf { lb -> lb != null } ?: ChocolateFactoryAPI.leaderboardPosition ?: return@put
                    val leaderboardChange = initialPosition - updatedPosition
                    val changeFormat = "${if (leaderboardChange < 0) "§c-" else "§a+"}$leaderboardChange"
                    it.appendHeadedLine(
                        "§7Leaderboard Change: §b#${stats.initLbPos} §c-> ${stats.finalLbPos}"
                            + if (leaderboardChange != 0) "§7($changeFormat spots§7)" else ""
                    )
                }

                put(HoppityStat.TIME_IN_CF) {
                    val timeFormat = stats.millisInCf.milliseconds.format(maxUnits = 2)
                    it.appendHeadedLine("§7You spent §b$timeFormat §7 in the §6Chocolate Factory§7.")
                }

                put(HoppityStat.EMPTY) { it.appendLine() }
                put(HoppityStat.EMPTY_2) { it.appendLine() }
                put(HoppityStat.EMPTY_3) { it.appendLine() }
                put(HoppityStat.EMPTY_4) { it.appendLine() }
            }
        }
    }

    private fun sendSummaryMessage(type: SummaryType, stats: HoppityEventStatsStorage) {
        val summaryBuilder: StringBuilder = StringBuilder()
        summaryBuilder.appendLine("§d§l${"▬".repeat(64)}")

        // Header
        summaryBuilder.appendLine("${" ".repeat(20)}§d§lHoppity's Hunt #${getHoppityEventNumber(stats.currentYear)} $type")
        summaryBuilder.appendLine()

        // Various stats from config
        config.eventSummary.statDisplayList.forEach {
            summaryOperationList[it]?.invoke(summaryBuilder)
        }

        // Footer
        summaryBuilder.appendLine("§d§l${"▬".repeat(64)}")
        ChatUtils.chat(summaryBuilder.toString(), prefix = false)
    }

    private fun HoppityEventStatsStorage.getEggsFoundFormat(): List<String> {
        val eggsFoundFormatList = mutableListOf<String>()
        val foundMealEggs = mealType.filterKeys { HoppityEggType.resettingEntries.contains(it) }.sumAllValues().toInt()
        if (foundMealEggs > 0) {
            val spawnedEggs = getMealEggsSinceStart()
            eggsFoundFormatList.add("§7You found §b$foundMealEggs§7/§a$spawnedEggs §6Chocolate Meal ${StringUtils.pluralize(foundMealEggs, "Egg")}§7.")
        }
        mealType[HoppityEggType.SIDE_DISH]?.let {
            eggsFoundFormatList.add("§7You found §b$it §6§lSide Dish §r§6${StringUtils.pluralize(it, "Egg")}§7 §7in the §dChocolate Factory§7.")
        }
        mealType[HoppityEggType.BOUGHT]?.let {
            eggsFoundFormatList.add("§7You bought §b$it §f${StringUtils.pluralize(it, "Rabbit")} §7from §aHoppity§7.")
        }

        if (eggsFoundFormatList.isEmpty()) {
            eggsFoundFormatList.add("§cNo Chocolate Eggs or Rabbits found during this event§7.")
        }
        return eggsFoundFormatList
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

    private fun getRabbitsFormat(rarityMap: Map<HoppityRabbitRarity, Int>, name: String, colorCode: String): List<String> {
        val formats = mutableListOf<String>()
        val rabbitsFound = rarityMap.toMutableMap()
        val rabbitsSum = rabbitsFound.sumAllValues().toInt()
        if (rabbitsSum == 0) return formats

        formats.add("§7$name Rabbits: $colorCode$rabbitsSum")

        var addSeparator = false
        val uniqueBuilder = StringBuilder()
        HoppityRabbitRarity.entries.forEach {
            if (addSeparator) uniqueBuilder.append(" §7-") else addSeparator = true
            uniqueBuilder.append(" ${it.colorCode}${rabbitsFound[it] ?: 0}")
        }

        formats.add(uniqueBuilder.toString())

        return formats
    }
}
