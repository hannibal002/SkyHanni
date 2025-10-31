package at.hannibal2.hanni.features.garden.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.GardenCropMilestones
import at.hannibal2.hanni.data.GardenCropMilestones.getCounter
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.features.garden.CropType
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.cleanName
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.formatPercentage
import at.hannibal2.hanni.utils.NumberUtil.toRoman
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.compat.setCustomItemName

// TODO: Merge common code with skill overflow
@HanniModule
object GardenInventoryTooltipOverflow {

    private val config get() = HanniMod.feature.garden.cropMilestones.overflow

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return

        val inventoryName = InventoryUtils.openInventoryName()
        if (inventoryName != "Crop Milestones") return

        val stack = event.itemStack
        if (!stack.getLore().any { it.contains("Max tier reached!") }) return

        val split = stack.cleanName().split(" ")
        val crop = getCrop(split)
        val counter = crop.getCounter()

        val currentTier = GardenCropMilestones.getTierForCropCount(counter, crop, allowOverflow = true)
        val (have, need) = getHaveNeed(currentTier, crop, counter)
        val (level, nextLevel) = getLevels(split, currentTier)

        var next = false
        val iterator = event.toolTip.listIterator()
        val percentage = have.toDouble() / need.toDouble()
        for (line in iterator) {
            val maxTierReached = "§7§8Max tier reached!"
            if (line.contains(maxTierReached)) {
                iterator.set("§7Progress to tier $nextLevel: §e${percentage.formatPercentage()}")
                event.itemStack.setCustomItemName("§a${crop.cropName} $level")
                next = true
                continue
            }
            if (next) {
                val bar = "                    "
                if (line.contains(bar)) {
                    val progressBar = StringUtils.progressBar(percentage)
                    iterator.set("$progressBar §e${have.addSeparators()}§6/§e${need.addSeparators()}")
                }
            }
        }
    }

    private fun getLevels(
        split: List<String>,
        currentTier: Int,
    ): Pair<String, String> {
        val nextTier = currentTier + 1
        val useRoman = split.last().toIntOrNull() == null
        val level = if (useRoman) currentTier.toRoman() else "" + currentTier
        val nextLevel = if (useRoman) nextTier.toRoman() else "" + nextTier
        return Pair(level, nextLevel)
    }

    private fun getHaveNeed(
        currentTier: Int,
        crop: CropType,
        counter: Long,
    ): Pair<Long, Long> {
        val nextTier = currentTier + 1
        val cropsForCurrentTier = GardenCropMilestones.getCropsForTier(currentTier, crop, allowOverflow = true)
        val cropsForNextTier = GardenCropMilestones.getCropsForTier(nextTier, crop, allowOverflow = true)
        val have = counter - cropsForCurrentTier
        val need = cropsForNextTier - cropsForCurrentTier
        return Pair(have, need)
    }

    private fun getCrop(split: List<String>): CropType {
        val cropName = split.dropLast(1).joinToString(" ")
        return CropType.getByName(cropName.removeColor())
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.inventoryTooltip
}
