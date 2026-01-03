package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getCurrentMilestoneTier
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getMilestoneCounter
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.milestoneTotalCropsForTier
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.garden.farming.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.indexOfFirst

@SkyHanniModule
object GardenCropMilestoneInventory {

    private var average: Double? = null
    private val config get() = GardenApi.config

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        // This should only render in the crop milestones menu, so no point updating it outside of that
        if (InventoryUtils.openInventoryName() != "Crop Milestones") return
        updateAverage()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (InventoryUtils.openInventoryName() != "Crop Milestones" || !config.number.averageCropMilestone) return
        if (average == null) updateAverage()

        if (event.slot.index == 2) {
            event.offsetY = -38
            event.offsetX = -50
            event.alignLeft = false
            event.stackTip = "§6Average Crop Milestone: §e$average"
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun addMaxMilestoneProgress(event: ToolTipEvent) {
        if (!config.tooltipTweak.cropMilestoneTotalProgress || InventoryUtils.openInventoryName() != "Crop Milestones") return

        val crop = CropMilestonesApi.getCropTypeByLore(event.itemStack) ?: return
        val tier = crop.getCurrentMilestoneTier() ?: return
        if (tier >= 20) return // Hypixel shows progress to ms46 after ms20

        val maxTier = CropMilestonesApi.getMaxTier()
        val maxCounter = crop.milestoneTotalCropsForTier(maxTier)

        val index = event.toolTipRemovedPrefix().indexOfFirst(
            "§7Rewards:",
        ) ?: return

        val counter = crop.getMilestoneCounter()?.toDouble() ?: return
        val percentage = counter / maxCounter
        val percentageFormat = percentage.formatPercentage()

        event.toolTip.add(index, " ")
        val progressBar = StringUtils.progressBar(percentage, 19)
        event.toolTip.add(index, "$progressBar §e${counter.addSeparators()}§6/§e${maxCounter.shortFormat()}")
        event.toolTip.add(index, "§7Progress to Tier $maxTier: §e$percentageFormat")
    }

    fun updateAverage() {
        if (!config.number.averageCropMilestone) return

        val tiers = mutableListOf<Double>()
        val allowOverflow = config.cropMilestones.overflow.inventoryStackSize
        for (cropType in CropType.entries) {
            val tier = cropType.getCurrentMilestoneTier() ?: continue
            if (!allowOverflow && tier > 46) {
                tiers.add(46.0)
            } else {
                tiers.add(tier.toDouble())
            }
        }
        average = (tiers.sum() / CropType.entries.size).roundTo(2)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.numberAverageCropMilestone", "garden.number.averageCropMilestone")
        event.move(3, "garden.cropMilestoneTotalProgress", "garden.tooltipTweak.cropMilestoneTotalProgress")
    }
}
