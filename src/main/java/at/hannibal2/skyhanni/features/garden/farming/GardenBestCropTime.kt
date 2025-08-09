package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.cropmilestones.NextConfig.BestTypeEntry
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.milestoneProgressToNextTier
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.getCurrentMilestoneTier
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.milestoneTierAmount
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.isMaxMilestone
import at.hannibal2.skyhanni.data.garden.GardenCropMilestones.milestoneTotalCropsForTier
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenNextJacobContest
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object GardenBestCropTime {

    var display: Renderable? = null

    private val config get() = GardenApi.config.cropMilestones

    val timeTillNextCrop = mutableMapOf<CropType, Duration>()

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onToggle(
            config.next.bestType,
            config.next.showOnlyBest,
            config.next.showCurrent,
            config.next.bestAlwaysOn,
            config.next.bestCompact,
            config.next.bestHideTitle,
        ) {
            val currentCrop = GardenApi.getCurrentlyFarmedCrop() ?: return@onToggle
            display = drawBestDisplay(currentCrop)
        }
    }

    fun reset() {
        timeTillNextCrop.clear()
        updateTimeTillNextCrop()
    }

    fun updateTimeTillNextCrop() {
        val useOverflow = config.overflow.bestCropTime
        for (crop in CropType.entries) {
            val speed = crop.getSpeed() ?: continue
            if (crop.isMaxMilestone() == true) continue

            val currentTier = crop.getCurrentMilestoneTier() ?: return

            val cropsForCurrentTier = crop.milestoneTotalCropsForTier(currentTier) ?: return

            val have = crop.milestoneProgressToNextTier() ?: return
            val need =
                if (config.bestShowMaxedNeeded.get()) {
                    cropsForCurrentTier
                } else {
                    crop.milestoneTierAmount(currentTier + 1) ?: return
                }

            val missing = need - have
            val missingTimeSeconds = missing / speed
            val millis = missingTimeSeconds * 1000
            timeTillNextCrop[crop] = millis.milliseconds
        }
    }

    fun drawBestDisplay(currentCrop: CropType?) = Renderable.vertical {
        if (timeTillNextCrop.size < CropType.entries.size) {
            updateTimeTillNextCrop()
        }

        val gardenExp = config.next.bestType.get() == BestTypeEntry.GARDEN_EXP
        val useOverflow = config.overflow.bestCropTime
        val sorted = if (gardenExp) {
            val helpMap = mutableMapOf<CropType, Long>()
            for ((crop, time) in timeTillNextCrop) {
                if (crop.isMaxMilestone() == true) continue
                val gardenExpForTier = getGardenExpForTier((crop.getCurrentMilestoneTier() ?: return@vertical) + 1)
                val fakeTime = time / gardenExpForTier
                helpMap[crop] = fakeTime.inWholeMilliseconds
            }
            helpMap.sorted()
        } else {
            timeTillNextCrop.sorted()
        }


        if (!config.next.bestHideTitle.get()) {
            val title = if (gardenExp) "§2Garden Experience" else "§bSkyBlock Level"
            if (config.next.bestCompact.get()) {
                addString("§eBest Crop Time")
            } else {
                addString("§eBest Crop Time §7($title§7)")
            }
        }

        if (!config.progress) {
            addString("§cCrop Milestone Progress Display is disabled!")
            return@vertical
        }

        if (sorted.isEmpty()) {
            addString("§cFarm crops to add them to this list!")
            return@vertical
        }

        sorted.keys.withIndex().forEach { (index, crop) ->
            createCropEntry(crop, index + 1, useOverflow, gardenExp, currentCrop)?.let(::add)
        }
    }

    private fun createCropEntry(crop: CropType, index: Int, useOverflow: Boolean, gardenExp: Boolean, currentCrop: CropType?): Renderable? {
        if (crop.isMaxMilestone() == true) return null
        val millis = timeTillNextCrop[crop] ?: return null
        val biggestUnit = config.highestTimeFormat.get().timeUnit
        val duration = millis.format(biggestUnit, maxUnits = 2)
        val isCurrent = crop == currentCrop
        if (index > config.next.showOnlyBest.get() && (!config.next.showCurrent.get() || !isCurrent)) return null

        return Renderable.horizontal {
            if (!config.next.bestCompact.get()) {
                addString("§7$index# ")
            }
            addItemStack(crop.icon)

            val color = if (isCurrent) "§e" else "§7"
            val contestFormat = if (GardenNextJacobContest.isNextCrop(crop)) "§n" else ""
            val currentTier = crop.getCurrentMilestoneTier() ?: return@horizontal
            val nextTier = if (config.bestShowMaxedNeeded.get()) 46 else currentTier + 1

            val cropName = if (!config.next.bestCompact.get()) crop.cropName + " " else ""
            val tier = if (!config.next.bestCompact.get()) "$currentTier➜$nextTier§r " else ""
            addString("$color$contestFormat$cropName$tier§b$duration")

            if (gardenExp && !config.next.bestCompact.get()) {
                val gardenExpForTier = getGardenExpForTier(nextTier)
                addString(" §7(§2$gardenExpForTier §7Exp)")
            }
        }
    }

    private fun getGardenExpForTier(gardenLevel: Int) = if (gardenLevel > 30) 300 else gardenLevel * 10

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.cropMilestoneBestType", "garden.cropMilestones.next.bestType")
        event.move(3, "garden.cropMilestoneShowOnlyBest", "garden.cropMilestones.next.showOnlyBest")
        event.move(3, "garden.cropMilestoneShowCurrent", "garden.cropMilestones.next.showCurrent")
        event.move(3, "garden.cropMilestoneBestCompact", "garden.cropMilestones.next.bestCompact")
        event.move(3, "garden.cropMilestoneBestHideTitle", "garden.cropMilestones.next.bestHideTitle")
    }
}
