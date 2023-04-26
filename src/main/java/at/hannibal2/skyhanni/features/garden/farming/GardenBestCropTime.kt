package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI.addCropIcon
import at.hannibal2.skyhanni.features.garden.GardenAPI.getSpeed
import at.hannibal2.skyhanni.features.garden.GardenNextJacobContest
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.TimeUtils

class GardenBestCropTime {
    var display = listOf<List<Any>>()
    private val config get() = SkyHanniMod.feature.garden

    companion object {
        val timeTillNextCrop = mutableMapOf<CropType, Long>()

        fun reset() {
            timeTillNextCrop.clear()
            updateTimeTillNextCrop()
        }

        fun updateTimeTillNextCrop() {
            for (crop in CropType.values()) {
                val speed = crop.getSpeed()
                if (speed == -1) continue

                val counter = crop.getCounter()
                val currentTier = GardenCropMilestones.getTierForCrops(counter)

                val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier)
                val nextTier = currentTier + 1
                val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier)

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
        if (timeTillNextCrop.size < CropType.values().size) {
            updateTimeTillNextCrop()
        }

        val gardenExp = config.cropMilestoneBestType == 0
        val sorted = if (gardenExp) {
            val helpMap = mutableMapOf<CropType, Long>()
            for ((cropName, time) in timeTillNextCrop) {
                val currentTier = GardenCropMilestones.getTierForCrops(cropName.getCounter())
                val gardenExpForTier = getGardenExpForTier(currentTier + 1)
                val fakeTime = time / gardenExpForTier
                helpMap[cropName] = fakeTime
            }
            helpMap.sorted()
        } else {
            timeTillNextCrop.sorted()
        }


        if (!config.cropMilestoneBestHideTitle) {
            val title = if (gardenExp) "§2Garden Experience" else "§bSkyBlock Level"
            if (config.cropMilestoneBestCompact) {
                newList.addAsSingletonList("§eBest Crop Time")
            } else {
                newList.addAsSingletonList("§eBest Crop Time §7($title§7)")
            }
        }

        if (!config.cropMilestoneProgress) {
            newList.addAsSingletonList("§cCrop Milestone Progress Display is disabled!")
            return newList
        }

        if (sorted.isEmpty()) {
            newList.addAsSingletonList("§cFarm crops to add them to this list!")
            return newList
        }

        var number = 0
        for (crop in sorted.keys) {
            val millis = timeTillNextCrop[crop]!!
            val duration = TimeUtils.formatDuration(millis, maxUnits = 2)
            val isCurrent = crop == currentCrop
            number++
            if (number > config.cropMilestoneShowOnlyBest && (!config.cropMilestoneShowCurrent || !isCurrent)) continue

            val list = mutableListOf<Any>()
            if (!config.cropMilestoneBestCompact) {
                list.add("§7$number# ")
            }
            list.addCropIcon(crop)

            val color = if (isCurrent) "§e" else "§7"
            val contestFormat = if (GardenNextJacobContest.isNextCrop(crop)) "§n" else ""
            val nextTier = GardenCropMilestones.getTierForCrops(crop.getCounter()) + 1

            val cropName = if (!config.cropMilestoneBestCompact) crop.cropName + " " else ""
            val cropNameDisplay = "$color$contestFormat$cropName$nextTier§r"
            list.add("$cropNameDisplay §b$duration")

            if (gardenExp && !config.cropMilestoneBestCompact) {
                val gardenExpForTier = getGardenExpForTier(nextTier)
                list.add(" §7(§2$gardenExpForTier §7Exp)")
            }
            newList.add(list)
        }
        return newList
    }

    private fun getGardenExpForTier(gardenLevel: Int) = if (gardenLevel > 30) 300 else gardenLevel * 10
}