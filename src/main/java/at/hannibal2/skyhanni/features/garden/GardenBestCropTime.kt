package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.TimeUtils
import java.util.*

class GardenBestCropTime {
    val display = mutableListOf<List<Any>>()
    val timeTillNextCrop = mutableMapOf<CropType, Long>()
    private val config get() = SkyHanniMod.feature.garden

    fun drawBestDisplay(currentCrop: CropType?) {
        if (timeTillNextCrop.size < GardenAPI.cropsPerSecond.size) {
            updateTimeTillNextCrop()
        }

        val gardenExp = config.cropMilestoneBestType == 0
        val sorted = if (gardenExp) {
            val helpMap = mutableMapOf<CropType, Long>()
            for ((cropName, time) in timeTillNextCrop) {
                val crops = GardenCropMilestones.cropCounter[cropName]!!
                val currentTier = GardenCropMilestones.getTierForCrops(crops)
                val gardenExpForTier = getGardenExpForTier(currentTier + 1)
                val fakeTime = time / gardenExpForTier
                helpMap[cropName] = fakeTime
            }
            helpMap.sorted()
        } else {
            timeTillNextCrop.sorted()
        }

        val title = if (gardenExp) "§2Garden Experience" else "§bSkyBlock Level"
        display.add(Collections.singletonList("§eBest Crop Time §7($title§7)"))

        if (sorted.isEmpty()) {
            display.add(Collections.singletonList("§cFarm crops to add them to this list!"))
        }

        var number = 0
        for (crop in sorted.keys) {
            val millis = timeTillNextCrop[crop]!!
            val duration = TimeUtils.formatDuration(millis)
            val isCurrent = crop == currentCrop
            number++
            if (number > config.cropMilestoneShowOnlyBest && !isCurrent) continue

            val list = mutableListOf<Any>()
            list.add("§7$number# ")
            GardenAPI.addGardenCropToList(crop, list)

            val color = if (isCurrent) "§e" else "§7"
            val contestFormat = if (GardenNextJacobContest.isNextCrop(crop)) "§n" else ""
            val crops = GardenCropMilestones.cropCounter[crop]!!
            val nextTier = GardenCropMilestones.getTierForCrops(crops) + 1
            val cropNameDisplay = "$color$contestFormat${crop.cropName} $nextTier§r"
            list.add("$cropNameDisplay §b$duration")

            if (gardenExp) {
                val gardenExpForTier = getGardenExpForTier(nextTier)
                list.add(" §7(§2$gardenExpForTier §7Exp)")
            }
            display.add(list)
        }
    }

    private fun getGardenExpForTier(gardenLevel: Int) = if (gardenLevel > 30) 300 else gardenLevel * 10

    fun updateTimeTillNextCrop() {
        for ((cropName, speed) in GardenAPI.cropsPerSecond) {
            if (speed == -1) continue

            val crops = GardenCropMilestones.cropCounter[cropName]!!
            val currentTier = GardenCropMilestones.getTierForCrops(crops)

            val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier)
            val nextTier = currentTier + 1
            val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier)

            val have = crops - cropsForCurrentTier
            val need = cropsForNextTier - cropsForCurrentTier

            val missing = need - have
            val missingTimeSeconds = missing / speed
            val millis = missingTimeSeconds * 1000
            timeTillNextCrop[cropName] = millis
        }
    }
}