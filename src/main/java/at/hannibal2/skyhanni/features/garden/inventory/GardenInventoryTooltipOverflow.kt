package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenInventoryTooltipOverflow {

    private val config get() = SkyHanniMod.feature.garden.cropMilestones.overflow

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        val inventoryName = InventoryUtils.openInventoryName()
        val stack = event.itemStack
        if (inventoryName == "Crop Milestones" && stack.getLore().any { it.contains("Max tier reached!") }) {
            val iterator = event.toolTip.listIterator()
            val split = stack.cleanName().split(" ")
            val useRoman = split.last().toIntOrNull() == null
            val cropName = split.dropLast(1).joinToString(" ")
            val crop = CropType.entries.firstOrNull { it.cropName == cropName.removeColor() } ?: return
            val counter = crop.getCounter()
            val currentTier = GardenCropMilestones.getTierForCropCount(counter, crop, allowOverflow = true)
            val nextTier = currentTier + 1
            val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier, crop, allowOverflow = true)
            val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier, crop, allowOverflow = true)
            val have = counter - cropsForCurrentTier
            val need = cropsForNextTier - cropsForCurrentTier
            var next = false
            val percent = LorenzUtils.formatPercentage(have.toDouble() / need.toDouble())
            for (line in iterator) {
                val maxTierReached = "§7§8Max tier reached!"
                if (line.contains(maxTierReached)) {
                    val level = if (useRoman) currentTier.toRoman() else currentTier
                    val nextLevel = if (useRoman) nextTier.toRoman() else nextTier
                    iterator.set("§7Progress to tier $nextLevel: §e$percent")
                    event.itemStack.name = "§a${crop.cropName} $level"
                    next = true
                    continue
                }
                if (next) {
                    val bar = "                    "
                    if (line.contains(bar)) {
                        val progressBar = StringUtils.progressBar(have.toDouble() / need.toDouble())
                        iterator.set("$progressBar §e${have.addSeparators()}§6/§e${need.addSeparators()}")
                    }
                }
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.inventoryTooltip
}
