package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.garden.farming.FarmingContestEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf

@SkyHanniModule
object JacobContestStatsSummary {

    private val config get() = GardenApi.config.jacobContest.contestSummary
    private val blocksBroken: MutableMap<CropType, Int> = enumMapOf()
    private var startTime = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN, priority = HandleEvent.HIGHEST)
    fun onCropClick(event: CropClickEvent) {
        if (!config.enabled) return
        if (event.clickType != ClickType.LEFT_CLICK) return

        if (FarmingContestApi.inContest && event.crop == FarmingContestApi.contestCrop) {
            blocksBroken.addOrPut(event.crop, 1)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onFarmingContest(event: FarmingContestEvent) {
        if (!config.enabled) return
        val cropsBroken = blocksBroken[event.crop] ?: 0
        if (event.phase == FarmingContestPhase.STOP && config.hideZeroCropStats && cropsBroken == 0) return

        when (event.phase) {
            FarmingContestPhase.START -> {
                ChatUtils.chat("Started tracking your Jacob Contest Blocks Per Second!")
                startTime = SimpleTimeMark.now()
            }

            FarmingContestPhase.STOP -> {
                val duration = startTime.passedSince()
                val blocksPerSecond = (cropsBroken.toDouble() / duration.inWholeSeconds).roundTo(2)
                val cropName = event.crop.cropName
                ChatUtils.chat("Stats for $cropName Contest:")
                val time = duration.format()
                ChatUtils.chat("§7Blocks Broken in total: §e${cropsBroken.addSeparators()}")
                val color = getBlocksPerSecondColor(blocksPerSecond)
                ChatUtils.chat("§7Average Blocks Per Second: $color$blocksPerSecond")
                ChatUtils.chat("§7Participated for §b$time")
            }

            FarmingContestPhase.CHANGE -> {
                ChatUtils.chat("You changed the crop during the contest, resetting the Blocks Per Second calculation..")
                startTime = SimpleTimeMark.now()
            }
        }
    }

    private fun getBlocksPerSecondColor(blocksPerSecond: Double) = if (blocksPerSecond > 19) "§c" else "§a"

    private val massMigrationPairs: List<Pair<String, String>> = listOf(
        "nextJacobContests" to "jacobContest.nextContest",
        "personalBests" to "jacobContest.personalBests",
        "farmingFortuneForContest" to "jacobContest.ffForContest",
        "farmingFortuneForContestPos" to "jacobContest.ffForContestPosition",
        "jacobContestTimes" to "jacobContest.timesNeeded.enabled",
        "jacobContestTimesPosition" to "jacobContest.timesNeeded.position",
        "jacobContestCustomBps" to "jacobContest.timesNeeded.customBps.enabled",
        "jacobContestCustomBpsValue" to "jacobContest.timesNeeded.customBps.value",
        "jacobContestSummary" to "jacobContest.contestSummary.enabled",
    )

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val oldBase = "garden"
        val newBase = "garden.jacobContest"
        massMigrationPairs.forEach { (oldPath, newPath) ->
            event.move(96, "$oldBase.$oldPath", "$newBase.$newPath")
        }
    }
}
