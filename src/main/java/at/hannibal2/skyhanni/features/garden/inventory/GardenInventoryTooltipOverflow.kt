package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getCurrentMilestoneTier
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.milestoneNextTierAmount
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.milestoneProgressToNextTier
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.setCustomItemName

// TODO: Merge common code with skill overflow
@SkyHanniModule
object GardenInventoryTooltipOverflow {

    private val config get() = GardenApi.config.cropMilestones.overflow

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return

        val inventoryName = InventoryUtils.openInventoryName()
        if (inventoryName != "Crop Milestones") return

        val stack = event.itemStack
        if (!stack.getLore().any { it.contains("Max tier reached!") }) return

        val split = stack.cleanName().split(" ")
        val crop = getCrop(split)

        val currentTier = crop.getCurrentMilestoneTier() ?: return
        val (have, need) = getHaveNeed(crop) ?: return
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
        crop: CropType,
    ): Pair<Long, Long>? {
        val have = crop.milestoneProgressToNextTier() ?: return null
        val need = crop.milestoneNextTierAmount() ?: return null
        return Pair(have, need)
    }

    private fun getCrop(split: List<String>): CropType {
        val cropName = split.dropLast(1).joinToString(" ")
        return CropType.getByName(cropName.removeColor())
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.inventoryTooltip
}
