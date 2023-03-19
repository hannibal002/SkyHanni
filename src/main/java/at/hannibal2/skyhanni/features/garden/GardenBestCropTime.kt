package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.TimeUtils
import java.util.*

class GardenBestCropTime {
    val display = mutableListOf<List<Any>>()
    val timeTillNextCrop = mutableMapOf<String, Long>()
    private val config get() = SkyHanniMod.feature.garden

    fun drawBestDisplay(currentCrop: String?) {
        if (timeTillNextCrop.size < GardenAPI.cropsPerSecond.size) {
            updateTimeTillNextCrop()
        }

        val gardenExp = config.cropMilestoneBestType == 0
        val sorted = if (gardenExp) {
            val helpMap = mutableMapOf<String, Long>()
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
        for (cropName in sorted.keys) {
            val millis = timeTillNextCrop[cropName]!!
            val duration = TimeUtils.formatDuration(millis)
            val isCurrent = cropName == currentCrop
            number++
            if (number > config.cropMilestoneShowOnlyBest && !isCurrent) continue

            val list = mutableListOf<Any>()
            list.add("§7$number# ")
            GardenAPI.addGardenCropToList(cropName, list)

            val color = if (isCurrent) "§e" else ""
            val contestFormat = if (GardenNextJacobContest.isNextCrop(cropName)) "§n" else ""
            val cropNameDisplay = "$color$contestFormat$cropName§r"
            list.add("$cropNameDisplay §b$duration")

            if (gardenExp) {
                val crops = GardenCropMilestones.cropCounter[cropName]!!
                val currentTier = GardenCropMilestones.getTierForCrops(crops)
                val gardenExpForTier = getGardenExpForTier(currentTier + 1)
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