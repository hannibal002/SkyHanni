package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
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
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyblockSeason
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object HoppityEventSummary {
    // First event was year 346 -> #1, 20th event was year 365, etc.
    private fun getHoppityEventNumber(skyblockYear: Int): Int = (skyblockYear - 345)
    private val config get() = SkyHanniMod.feature.event.hoppityEggs

    @HandleEvent
    fun onRabbitFound(event: RabbitFoundEvent) {
        if (!HoppityAPI.isHoppityEvent()) return
        val stats = ProfileStorageData.profileSpecific?.hoppityEventStats ?: return

        stats.mealTypeMap.addOrPut(event.eggType, 1)
        val rarity = HoppityRabbitRarity.getByRabbit(event.rabbitName) ?: return
        if (event.duplicate) stats.dupeRarityMap.addOrPut(rarity, 1)
        else stats.newRarityMap.addOrPut(rarity, 1)
        if (event.chocGained > 0) stats.chocolateGained += event.chocGained
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        checkEnded()
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        checkEnded()
    }

    fun forceEventEnd() {
        ProfileStorageData.profileSpecific?.hoppityEventStats?.currentYear ?: return
        val currentYear = SkyBlockTime.now().year
        ProfileStorageData.profileSpecific?.hoppityEventStats!!.currentYear = currentYear - 1
    }

    private fun checkEnded() {
        if (!config.eventSummary) return
        val stats = ProfileStorageData.profileSpecific?.hoppityEventStats ?: return

        val currentYear = SkyBlockTime.now().year
        val currentSeason = SkyblockSeason.currentSeason

        if (stats == HoppityEventStatsStorage()) {
            ProfileStorageData.profileSpecific!!.hoppityEventStats!!.currentYear = currentYear
            return
        }

        val ended = stats.currentYear <= currentYear || (stats.currentYear == currentYear && currentSeason != SkyblockSeason.SPRING)
        if (ended) {
            sendSummaryMessage(SummaryType.CONCLUDED, stats)
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

    private fun sendSummaryMessage(type: SummaryType, stats: HoppityEventStatsStorage) {
        val headerMessage = "§d§lHoppity's Hunt #${getHoppityEventNumber(stats.currentYear)} $type"
        val headerLength = headerMessage.removeColor().length
        val wrapperLength = ((headerLength + 8) * 1.5).toInt()
        val summaryWrapper = "§d§l${"▬".repeat(wrapperLength + 4)}"
        val lineHeader = " ".repeat(4)

        val summaryBuilder: StringBuilder = StringBuilder()
        summaryBuilder.appendLine(summaryWrapper)
        summaryBuilder.appendLine("${" ".repeat(((summaryWrapper.length - 4) / 2) - 8)}$headerMessage")
        summaryBuilder.appendLine()
        stats.getEggsFoundFormat().forEach {
            summaryBuilder.appendLine("$lineHeader$it")
        }
        summaryBuilder.appendLine()
        getRabbitsFormat(stats.newRarityMap, "Unique", "§b").forEach {
            summaryBuilder.appendLine("$lineHeader$it")
        }
        summaryBuilder.appendLine()
        getRabbitsFormat(stats.dupeRarityMap, "Duplicate", "§c").forEach {
            summaryBuilder.appendLine("$lineHeader$it")
        }
        if (stats.chocolateGained > 0) {
            var extraChocFormat = "§6+${stats.chocolateGained.addSeparators()} Chocolate";
            if (SkyHanniMod.feature.inventory.chocolateFactory.showDuplicateTime) {
                val timeFormatted = ChocolateFactoryAPI.timeUntilNeed(stats.chocolateGained).format(maxUnits = 2)
                extraChocFormat += " §7(§a+§b${timeFormatted}§7)"
            }
            summaryBuilder.appendLine("$lineHeader$extraChocFormat")
        }
        summaryBuilder.appendLine(summaryWrapper)

        ChatUtils.chat(summaryBuilder.toString(), prefix = false)
    }

    private fun HoppityEventStatsStorage.getEggsFoundFormat(): List<String> {
        val eggsFoundFormatList = mutableListOf<String>()
        val foundMealEggs = mealTypeMap.filterKeys { HoppityEggType.resettingEntries.contains(it) }.sumAllValues().toInt()
        if (foundMealEggs > 0) {
            eggsFoundFormatList.add("§7You found §b$foundMealEggs §6Chocolate Egg${if(foundMealEggs > 1) "s" else ""}§7.")
        }
        mealTypeMap[HoppityEggType.SIDE_DISH]?.let {
            eggsFoundFormatList.add("§7You found §b$it §6§lSide Dish §r§6Egg${if(it > 1) "s" else ""}§7 §7in the §dChocolate Factory§7.")
        }
        mealTypeMap[HoppityEggType.BOUGHT]?.let {
            eggsFoundFormatList.add("§7You bought §b$it §fRabbit${if(it > 1) "s" else ""} §7from §aHoppity§7.")
        }

        if (eggsFoundFormatList.isEmpty()) {
            eggsFoundFormatList.add("§cNo Chocolate Eggs or Rabbits found during this event§7.")
        }
        return eggsFoundFormatList
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
            if(addSeparator) uniqueBuilder.append(" §7-") else addSeparator = true
            uniqueBuilder.append(" ${it.colorCode}${rabbitsFound[it] ?: 0}")
        }

        formats.add(uniqueBuilder.toString())

        return formats
    }
}
