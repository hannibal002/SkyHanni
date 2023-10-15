package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.isMaxed
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI.addCropIcon
import at.hannibal2.skyhanni.features.garden.GardenNextJacobContest
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenBestCropTime {
    var display = emptyList<List<Any>>()

    companion object {
        private val config get() = SkyHanniMod.feature.garden.cropMilestones
        val timeTillNextCrop = mutableMapOf<CropType, Long>()

        fun reset() {
            timeTillNextCrop.clear()
            updateTimeTillNextCrop()
        }

        fun updateTimeTillNextCrop() {
            for (crop in CropType.entries) {
                val speed = crop.getSpeed() ?: continue
                if (crop.isMaxed()) continue

                val counter = crop.getCounter()
                val currentTier = GardenCropMilestones.getTierForCropCount(counter, crop)

                val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier, crop)
                val nextTier = if (config.bestShowMaxedNeeded.get()) 46 else currentTier + 1
                val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier, crop)

                val have = counter - cropsForCurrentTier
                val need = cropsForNextTier - cropsForCurrentTier

                val missing = need - have
                val missingTimeSeconds = missing / speed
                val millis = missingTimeSeconds * 1000
                timeTillNextCrop[crop] = millis
            }
        }
    }

    fun drawBestDisplay(currentCrop: CropType?): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        if (timeTillNextCrop.size < CropType.entries.size) {
            updateTimeTillNextCrop()
        }

        val gardenExp = config.next.bestType == 0
        val sorted = if (gardenExp) {
            val helpMap = mutableMapOf<CropType, Long>()
            for ((crop, time) in timeTillNextCrop) {
                if (crop.isMaxed()) continue
                val currentTier = GardenCropMilestones.getTierForCropCount(crop.getCounter(), crop)
                val gardenExpForTier = getGardenExpForTier(currentTier + 1)
                val fakeTime = time / gardenExpForTier
                helpMap[crop] = fakeTime
            }
            helpMap.sorted()
        } else {
            timeTillNextCrop.sorted()
        }


        if (!config.next.bestHideTitle) {
            val title = if (gardenExp) "§2Garden Experience" else "§bSkyBlock Level"
            if (config.next.bestCompact) {
                newList.addAsSingletonList("§eBest Crop Time")
            } else {
                newList.addAsSingletonList("§eBest Crop Time §7($title§7)")
            }
        }

        if (!config.progress) {
            newList.addAsSingletonList("§cCrop Milestone Progress Display is disabled!")
            return newList
        }

        if (sorted.isEmpty()) {
            newList.addAsSingletonList("§cFarm crops to add them to this list!")
            return newList
        }

        var number = 0
        for (crop in sorted.keys) {
            if (crop.isMaxed()) continue
            val millis = timeTillNextCrop[crop]!!
            val biggestUnit = TimeUnit.entries[config.highestTimeFormat.get()]
            val duration = TimeUtils.formatDuration(millis, biggestUnit, maxUnits = 2)
            val isCurrent = crop == currentCrop
            number++
            if (number > config.next.showOnlyBest && (!config.next.showCurrent || !isCurrent)) continue

            val list = mutableListOf<Any>()
            if (!config.next.bestCompact) {
                list.add("§7$number# ")
            }
            list.addCropIcon(crop)

            val color = if (isCurrent) "§e" else "§7"
            val contestFormat = if (GardenNextJacobContest.isNextCrop(crop)) "§n" else ""
            val currentTier = GardenCropMilestones.getTierForCropCount(crop.getCounter(), crop)
            val nextTier = if (config.bestShowMaxedNeeded.get()) 46 else currentTier + 1


            val cropName = if (!config.next.bestCompact) crop.cropName + " " else ""
            val tier = if (!config.next.bestCompact) "$currentTier➜$nextTier§r " else ""
            list.add("$color$contestFormat$cropName$tier§b$duration")

            if (gardenExp && !config.next.bestCompact) {
                val gardenExpForTier = getGardenExpForTier(nextTier)
                list.add(" §7(§2$gardenExpForTier §7Exp)")
            }
            newList.add(list)
        }
        return newList
    }

    private fun getGardenExpForTier(gardenLevel: Int) = if (gardenLevel > 30) 300 else gardenLevel * 10

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.cropMilestoneBestType", "garden.cropMilestones.next.bestType")
        event.move(3, "garden.cropMilestoneShowOnlyBest", "garden.cropMilestones.next.showOnlyBest")
        event.move(3, "garden.cropMilestoneShowCurrent", "garden.cropMilestones.next.showCurrent")
        event.move(3, "garden.cropMilestoneBestCompact", "garden.cropMilestones.next.bestCompact")
        event.move(3, "garden.cropMilestoneBestHideTitle", "garden.cropMilestones.next.bestHideTitle")
    }
}