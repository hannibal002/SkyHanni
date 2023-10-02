package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.indexOfFirst
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenCropMilestoneInventory {
    private var average = -1.0
    private val config get() = SkyHanniMod.feature.garden

    @SubscribeEvent
    fun onCropMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        if (!config.numbers.averageCropMilestone) return

        val tiers = mutableListOf<Double>()
        for (cropType in CropType.entries) {
            val counter = cropType.getCounter()
            val tier = GardenCropMilestones.getTierForCropCount(counter, cropType)
            tiers.add(tier.toDouble())
        }
        average = (tiers.sum() / CropType.entries.size).round(2)
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        average = -1.0
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (average == -1.0) return

        if (event.slot.slotNumber == 38) {
            event.offsetY = -23
            event.offsetX = -50
            event.alignLeft = false
            event.stackTip = "§6Average Crop Milestone: §e$average"
        }
    }

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.tooltipTweaks.cropMilestoneTotalProgress) return

        val itemStack = event.itemStack ?: return
        val crop = GardenCropMilestones.getCropTypeByLore(itemStack) ?: return

        val maxTier = GardenCropMilestones.getMaxTier()
        val maxCounter = GardenCropMilestones.getCropsForTier(maxTier, crop)

        val index = event.toolTip.indexOfFirst(
            "§5§o§7Rewards:",
        ) ?: return

        val counter = crop.getCounter().toDouble()
        val percentage = counter / maxCounter
        val percentageFormat = LorenzUtils.formatPercentage(percentage)

        event.toolTip.add(index, " ")
        val progressBar = StringUtils.progressBar(percentage)
        event.toolTip.add(index, "$progressBar §e${counter.addSeparators()}§6/§e${NumberUtil.format(maxCounter)}")
        event.toolTip.add(index, "§7Progress to Tier $maxTier: §e$percentageFormat")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent){
        event.move(4,"garden.numberAverageCropMilestone", "garden.numbers.averageCropMilestone")
        event.move(4, "garden.cropMilestoneTotalProgress", "garden.tooltipTweaks.cropMilestoneTotalProgress")
    }
}