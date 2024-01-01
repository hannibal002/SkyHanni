package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraft.command.CommandBase

object FarmingMilestoneCommand {

    fun onCommand(crop: String?, current: String?, target: String?) {
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
            val cropsNeeded = GardenCropMilestones.getCropsForTier(currentCropMilestone, enteredCrop) - currentProgress

            LorenzUtils.chat("§a${enteredCrop.cropName} needed for you to reach the next milestone ($currentCropMilestone) is ${cropsNeeded.addSeparators()}")
            return
        }

        if (targetMilestone == null) {
            val cropsNeeded = GardenCropMilestones.getCropsForTier(currentMilestone, enteredCrop)

            LorenzUtils.chat("§a${enteredCrop.cropName} needed to reach milestone $currentMilestone is ${cropsNeeded.addSeparators()}")
            return
        }
        if (currentMilestone >= targetMilestone) {
            LorenzUtils.chat("Entered milestone is greater than target milestone")
            return
        }

        val currentAmount = GardenCropMilestones.getCropsForTier(currentMilestone, enteredCrop)
        val targetAmount = GardenCropMilestones.getCropsForTier(targetMilestone, enteredCrop)
        val cropsNeeded = targetAmount - currentAmount

        LorenzUtils.chat("§a${enteredCrop.cropName} needed to reach milestone $targetMilestone from milestone $currentMilestone is ${cropsNeeded.addSeparators()}")
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
}
