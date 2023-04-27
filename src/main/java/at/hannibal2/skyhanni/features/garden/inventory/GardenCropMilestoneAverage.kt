package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenCropMilestoneAverage {
    private var average = -1.0

    @SubscribeEvent
    fun onCropMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        if (!SkyHanniMod.feature.garden.numberAverageCropMilestone) return

        val tiers = mutableListOf<Double>()
        for (cropType in CropType.values()) {
            val counter = cropType.getCounter()
            val tier = GardenCropMilestones.getTierForCrops(counter)
            tiers.add(tier.toDouble())
        }
        average = tiers.average().round(2)
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
}