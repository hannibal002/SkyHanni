package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.setCounter
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class GardenCropMilestoneFix {
    private val tabListPattern = " Milestone: §r§a(?<crop>.*) (?<tier>.*): §r§3(?<percentage>.*)%".toPattern()
    private val levelUpPattern = Pattern.compile("  §r§b§lGARDEN MILESTONE §3(?<crop>.*) §8(?:.*)➜§3(?<tier>.*)")

    private val tabListCropProgress = mutableMapOf<CropType, Long>()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val matcher = levelUpPattern.matcher(event.message)
        if (!matcher.matches()) return

        val cropName = matcher.group("crop")
        val crop = CropType.getByNameOrNull(cropName)
        if (crop == null) {
            LorenzUtils.debug("GardenCropMilestoneFix: crop is null: '$cropName'")
            return
        }

        val tier = matcher.group("tier").romanToDecimalIfNeeded()

        val crops = GardenCropMilestones.getCropsForTier(tier)
        changedValue(crop, crops, "level up chat message")
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        for (line in event.tabList) {
            val matcher = tabListPattern.matcher(line)
            if (!matcher.matches()) continue

            val tier = matcher.group("tier").toInt()
            val percentage = matcher.group("percentage").toDouble()
            val cropName = matcher.group("crop")

            check(cropName, tier, percentage)
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

        val newValue = tabListValue.toLong()
        if (tabListCropProgress[crop] != newValue) {
            if (tabListCropProgress.containsKey(crop)) {
                changedValue(crop, newValue, "tab list")
            }
        }
        tabListCropProgress[crop] = newValue
    }

    private fun changedValue(crop: CropType, tabListValue: Long, source: String) {
        val calculated = crop.getCounter()
        val diff = calculated - tabListValue
        if (diff < -5_000) {
            crop.setCounter(tabListValue)
            LorenzUtils.chat("§e[SkyHanni] Loaded ${crop.cropName} milestone data from $source!")
        }
        if (diff > 5_000) {
            LorenzUtils.debug("Fixed wrong ${crop.cropName} milestone data from $source: ${diff.addSeparators()}")
            crop.setCounter(tabListValue)
        }
    }
}