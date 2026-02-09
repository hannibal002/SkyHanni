package at.hannibal2.skyhanni.data.garden.cropmilestones

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.addMilestoneCounter
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.clearMilestoneCache
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getCropTypeByLore
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getMilestoneCounter
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.inaccurateMilestone
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.isMaxMilestone
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.levelUpPattern
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.milestoneTotalCropsForTier
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.storage
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.tabListMaxPattern
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.tabListPercentPattern
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.totalPattern
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.farming.GardenCropMilestoneDisplay
import at.hannibal2.skyhanni.features.garden.inventory.GardenCropMilestoneInventory
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark

@SkyHanniModule
object WrongDataFix {
    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Crop Milestones") return

        clearMilestoneCache()
        for ((_, stack) in event.inventoryItems) {
            val crop = getCropTypeByLore(stack) ?: continue
            totalPattern.firstMatcher(stack.getLore()) {
                val oldAmount = crop.getMilestoneCounter() ?: 0
                val amount = group("name").formatLong()
                val change = amount - oldAmount
                forceUpdateMilestone(crop, change)
            }
        }
        storage?.lastMilestoneFix = SimpleTimeMark.now()
        inaccurateMilestone = false
        CommunityFix.openInventory(event.inventoryItems)
        GardenCropMilestoneInventory.updateAverage()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        levelUpPattern.matchMatcher(event.message) {
            val cropName = group("crop")
            val crop = CropType.getByNameOrNull(cropName) ?: return
            val tier = group("tier").romanToDecimalIfNecessary()
            val crops = crop.milestoneTotalCropsForTier(tier)
            changedValue(crop, crops, "level up chat message", 0)
        }
    }

    @HandleEvent
    fun onTabListUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.CROP_MILESTONE)) return
        tabListPercentPattern.firstMatcher(event.lines) {
            val tier = group("tier").toInt()
            val percentage = group("percentage").toDouble()
            val cropName = group("crop")

            checkTabDifference(cropName, tier, percentage)
        }
        tabListMaxPattern.firstMatcher(event.lines) {
            val tier = group("tier").toInt()
            val cropName = group("crop")

            setCropToMaxTier(cropName, tier)
        }
    }

    private val tabListCropProgress = mutableMapOf<CropType, Long>()
    private val loadedCrops = mutableListOf<CropType>()

    private fun changedValue(crop: CropType, tabListValue: Long, source: String, minDiff: Int) {
        val calculated = crop.getMilestoneCounter() ?: return
        val diff = tabListValue - calculated

        if (diff >= minDiff) {
            forceUpdateMilestone(crop, diff)
            storage?.lastMilestoneFix = SimpleTimeMark.now()
            GardenCropMilestoneDisplay.update()
            if (!loadedCrops.contains(crop)) {
                loadedCrops.add(crop)
            }
        } else if (diff <= minDiff) {
            ChatUtils.debug("Fixed wrong ${crop.cropName} milestone data from $source: ${diff.addSeparators()}")
        }
    }

    private fun checkTabDifference(cropName: String, tier: Int, percentage: Double) {
        if (!ProfileStorageData.loaded) return

        val crop = CropType.getByNameOrNull(cropName)
        if (crop == null) {
            ChatUtils.debug("GardenCropMilestoneFix: crop is null: '$cropName'")
            return
        }

        val baseCrops = crop.milestoneTotalCropsForTier(tier)
        val next = crop.milestoneTotalCropsForTier(tier + 1)
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

    private fun setCropToMaxTier(cropName: String, tier: Int) {
        val crop = CropType.getByNameOrNull(cropName)
        if (crop == null) {
            ChatUtils.debug("GardenCropMilestoneFix: crop is null: '$cropName'")
            return
        }

        val cropAmount = crop.milestoneTotalCropsForTier(tier)
        if (!crop.isMaxMilestone()) {
            val oldAmount = crop.getMilestoneCounter() ?: 0
            val change = cropAmount - oldAmount
            if (change < 0) return
            forceUpdateMilestone(crop, change)
        }
    }

    private fun forceUpdateMilestone(crop: CropType, amount: Long) {
        if (amount == 0L) return
        ChatUtils.debug("Force Updating Milestone: Crop: $crop, Amount: ${amount.addSeparators()}")
        crop.addMilestoneCounter(amount, false)
    }
}
