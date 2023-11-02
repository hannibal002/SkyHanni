package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.setCounter
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.garden.farming.GardenCropMilestoneDisplay
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class GardenCropMilestoneFix {
    private val tabListPattern = " Milestone: §r§a(?<crop>.*) (?<tier>.*): §r§3(?<percentage>.*)%".toPattern()
    private val levelUpPattern = Pattern.compile(" {2}§r§b§lGARDEN MILESTONE §3(?<crop>.*) §8(?:.*)➜§3(?<tier>.*)")

    private val tabListCropProgress = mutableMapOf<CropType, Long>()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        levelUpPattern.matchMatcher(event.message) {
            val cropName = group("crop")
            val crop = CropType.getByNameOrNull(cropName) ?: return

            val tier = group("tier").romanToDecimalIfNeeded()

            val crops = GardenCropMilestones.getCropsForTier(tier, crop)
            changedValue(crop, crops, "level up chat message", 0)
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        for (line in event.tabList) {
            tabListPattern.matchMatcher(line) {
                val tier = group("tier").toInt()
                val percentage = group("percentage").toDouble()
                val cropName = group("crop")

                check(cropName, tier, percentage)
                return
            }
        }
    }

    private fun check(cropName: String, tier: Int, percentage: Double) {
        if (!ProfileStorageData.loaded) return

        val crop = CropType.getByNameOrNull(cropName)
        if (crop == null) {
            LorenzUtils.debug("GardenCropMilestoneFix: crop is null: '$cropName'")
            return
        }

        val baseCrops = GardenCropMilestones.getCropsForTier(tier, crop)
        val next = GardenCropMilestones.getCropsForTier(tier + 1, crop)
        val progressCrops = next - baseCrops

        val progress = progressCrops * (percentage / 100)
        val smallestPercentage = progressCrops * 0.0005

        val tabListValue = baseCrops + progress - smallestPercentage

        val newValue = tabListValue.toLong()
        if (tabListCropProgress[crop] != newValue && tabListCropProgress.containsKey(crop)) {
            changedValue(crop, newValue, "tab list", smallestPercentage.toInt())
        }
        tabListCropProgress[crop] = newValue
    }

    private val loadedCrops = mutableListOf<CropType>()

    private fun changedValue(crop: CropType, tabListValue: Long, source: String, minDiff: Int) {
        val calculated = crop.getCounter()
        val diff = calculated - tabListValue

        if (diff <= -minDiff) {
            crop.setCounter(tabListValue)
            GardenCropMilestoneDisplay.update()
            if (!loadedCrops.contains(crop)) {
                LorenzUtils.chat("§e[SkyHanni] Loaded ${crop.cropName} milestone data from $source!")
                loadedCrops.add(crop)
            }
        } else if (diff >= minDiff) {
            LorenzUtils.debug("Fixed wrong ${crop.cropName} milestone data from $source: ${diff.addSeparators()}")
            crop.setCounter(tabListValue)
            GardenCropMilestoneDisplay.update()
        }
    }
}
