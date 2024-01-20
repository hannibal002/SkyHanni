package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.command.CommandBase

object FarmingMilestoneCommand {

    fun onCommand(crop: String?, current: String?, target: String?, needsTime: Boolean) {
        if (crop == null) {
            LorenzUtils.userError("No crop type entered")
            return
        }

        val enteredCrop = CropType.entries.firstOrNull { it.simpleName == crop.lowercase() } ?: run {
            LorenzUtils.userError("Invalid crop type entered")
            return
        }

        val currentMilestone = getValidNumber(current)
        val targetMilestone = getValidNumber(target)

        if (currentMilestone == null) {
            val currentProgress = enteredCrop.getCounter()
            val currentCropMilestone = GardenCropMilestones.getTierForCropCount(currentProgress, enteredCrop) + 1
            val cropsForTier = GardenCropMilestones.getCropsForTier(currentCropMilestone, enteredCrop)
            val output = (cropsForTier - currentProgress).formatOutput(needsTime, enteredCrop)

            LorenzUtils.chat("§7$output needed to reach the next milestone")
            return
        }

        if (targetMilestone == null) {
            val cropsForTier = GardenCropMilestones.getCropsForTier(currentMilestone, enteredCrop)
            val output = cropsForTier.formatOutput(needsTime, enteredCrop)

            LorenzUtils.chat("§7$output needed for milestone §7$currentMilestone")
            return
        }
        if (currentMilestone >= targetMilestone) {
            LorenzUtils.userError("Entered milestone is greater than or the same as target milestone")
            return
        }

        val currentAmount = GardenCropMilestones.getCropsForTier(currentMilestone, enteredCrop)
        val targetAmount = GardenCropMilestones.getCropsForTier(targetMilestone, enteredCrop)
        val output = (targetAmount - currentAmount).formatOutput(needsTime, enteredCrop)

        LorenzUtils.chat("§7$output needed for milestone §7$currentMilestone §a-> §7$targetMilestone")
    }

    fun onComplete(strings: Array<String>): List<String> {
        return if (strings.size <= 1) {
            CommandBase.getListOfStringsMatchingLastWord(
                strings,
                CropType.entries.map { it.simpleName }
            )
        } else listOf()
    }

    private fun getValidNumber(entry: String?) = entry?.toIntOrNull()?.coerceIn(0, 46)

    private fun Long.formatOutput(needsTime: Boolean, crop: CropType): String {
        if (!needsTime) return "${this.addSeparators()} §a${crop.cropName}"
        val speed = crop.getSpeed() ?: -1
        val missingTimeSeconds = this / speed
        return "${TimeUtils.formatDuration(missingTimeSeconds * 1000)}§a"
    }
}
