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
            LorenzUtils.chat("No crop type entered")
            return
        }

        val enteredCrop = CropType.entries.firstOrNull { it.simpleName == crop.lowercase() } ?: run {
            LorenzUtils.chat("Invalid crop type entered")
            return
        }

        val currentMilestone = getValidNumber(current)
        val targetMilestone = getValidNumber(target)

        if (currentMilestone == null) {
            val currentProgress = enteredCrop.getCounter()
            val currentCropMilestone = GardenCropMilestones.getTierForCropCount(currentProgress, enteredCrop) + 1
            val output = (GardenCropMilestones.getCropsForTier(currentCropMilestone, enteredCrop) - currentProgress).formatOutput(needsTime, enteredCrop)

            LorenzUtils.chat("§7$output needed for you to reach the next milestone")
            return
        }

        if (targetMilestone == null) {
            val output = GardenCropMilestones.getCropsForTier(currentMilestone, enteredCrop).formatOutput(needsTime, enteredCrop)

            LorenzUtils.chat("§7$output needed for milestone §7$currentMilestone")
            return
        }
        if (currentMilestone >= targetMilestone) {
            LorenzUtils.chat("Entered milestone is greater than target milestone")
            return
        }

        val currentAmount = GardenCropMilestones.getCropsForTier(currentMilestone, enteredCrop)
        val targetAmount = GardenCropMilestones.getCropsForTier(targetMilestone, enteredCrop)
        val output = (targetAmount - currentAmount).formatOutput(needsTime, enteredCrop)

        LorenzUtils.chat("§7$output needed for milestone §7$currentMilestone §a-> §7$targetMilestone")
    }

    fun onComplete(strings: Array<String>): List<String> {
        if (strings.size <= 1)
            return CommandBase.getListOfStringsMatchingLastWord(
                strings,
                CropType.entries.map { it.simpleName }
            )
        return listOf()
    }

    private fun getValidNumber(entry: String?): Int? {
        if (entry == null) return null
        var result = try {
            entry.toInt()
        } catch (_: Exception) {
            null
        }
        if (result == null) return null

        if (result < 0) result = 0
        if (result > 46) result = 46

        return result
    }

    private fun Long.formatOutput(needsTime: Boolean, crop: CropType): String {
        if (!needsTime) return "${this.addSeparators()} §a${crop.cropName}"
        val speed = crop.getSpeed() ?: -1
        val missingTimeSeconds = this / speed
        return "${TimeUtils.formatDuration(missingTimeSeconds * 1000)}§a"
    }
}
