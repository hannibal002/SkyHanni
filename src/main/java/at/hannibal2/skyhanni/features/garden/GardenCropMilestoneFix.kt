package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.setCounter
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenCropMilestoneFix {
    private val pattern = " Milestone: §r§a(?<crop>.*) (?<tier>.*): §r§3(?<percentage>.*)%".toPattern()

    private val tabListCropProgress = mutableMapOf<CropType, Long>()

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        for (line in event.tabList) {
            val matcher = pattern.matcher(line)
            if (!matcher.matches()) continue

            val tier = matcher.group("tier").toInt()
            val percentage = matcher.group("percentage").toDouble()

            check(matcher.group("crop"), tier, percentage)
            return
        }
    }

    private fun check(cropName: String, tier: Int, percentage: Double) {
        val baseCrops = GardenCropMilestones.getCropsForTier(tier)
        val next = GardenCropMilestones.getCropsForTier(tier + 1)
        val progressCrops = next - baseCrops

        val progress = progressCrops * (percentage / 100)
        val smallestPercentage = progressCrops * 0.0005

        val crop = CropType.getByNameOrNull(cropName)
        if (crop == null) {
            LorenzUtils.debug("GardenCropMilestoneFix: crop is null: '$cropName'")
            return
        }

        val tabListValue = baseCrops + progress - smallestPercentage

        val long = tabListValue.toLong()

        if (tabListCropProgress[crop] == long) return
        if (tabListCropProgress.containsKey(crop)) {
            changedValue(crop, long)
        }

        tabListCropProgress[crop] = long
    }

    private fun changedValue(crop: CropType, tabListValue: Long) {
        val calculated = crop.getCounter()
        val diff = calculated - tabListValue
        if (diff < -5_000) {
            crop.setCounter(tabListValue)
            LorenzUtils.chat("§e[SkyHanni] Loaded ${crop.cropName} milestone data from tab list!")
        }
        if (diff > 5_000) {
            LorenzUtils.debug("Fixed wrong ${crop.cropName} milestone data from tab list: ${diff.addSeparators()}")
            crop.setCounter(tabListValue)
        }
    }
}