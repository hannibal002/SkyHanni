package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrUserError
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FarmingMilestoneCommand {

    fun onCommand(crop: String?, current: String?, target: String?, needsTime: Boolean) {
        if (crop == null) {
            ChatUtils.userError("No crop type entered")
            return
        }

        val enteredCrop = CropType.getByNameOrNull(crop) ?: run {
            ChatUtils.userError("Invalid crop type entered")
            return
        }

        val currentMilestone = current?.toIntOrNull()
        val targetMilestone = target?.toIntOrNull()

        if (currentMilestone == null) {
            val currentProgress = enteredCrop.getCounter()
            val currentCropMilestone =
                GardenCropMilestones.getTierForCropCount(currentProgress, enteredCrop, allowOverflow = true) + 1
            val cropsForTier =
                GardenCropMilestones.getCropsForTier(currentCropMilestone, enteredCrop, allowOverflow = true)
            val output = (cropsForTier - currentProgress).formatOutput(needsTime, enteredCrop)

            ChatUtils.chat("§7$output needed to reach the next milestone")
            return
        }

        if (targetMilestone == null) {
            val cropsForTier = GardenCropMilestones.getCropsForTier(currentMilestone, enteredCrop, allowOverflow = true)
            val output = cropsForTier.formatOutput(needsTime, enteredCrop)

            ChatUtils.chat("§7$output needed for milestone §7$currentMilestone")
            return
        }

        if (currentMilestone >= targetMilestone) {
            ChatUtils.userError("Entered milestone is greater than or the same as target milestone")
            return
        }

        val currentAmount = GardenCropMilestones.getCropsForTier(currentMilestone, enteredCrop, allowOverflow = true)
        val targetAmount = GardenCropMilestones.getCropsForTier(targetMilestone, enteredCrop, allowOverflow = true)
        val output = (targetAmount - currentAmount).formatOutput(needsTime, enteredCrop)
        ChatUtils.chat("§7$output needed for milestone §7$currentMilestone §a-> §7$targetMilestone")
    }

    fun setGoal(args: Array<String>) {
        val storage = ProfileStorageData.profileSpecific?.garden?.customGoalMilestone ?: return

        if (args.size != 2) {
            ChatUtils.userError("Usage: /shcropgoal <crop name> <target milestone>")
            return
        }

        val enteredCrop = CropType.getByNameOrNull(args[0]) ?: run {
            ChatUtils.userError("Not a crop type: '${args[0]}'")
            return
        }
        val targetLevel = args[1].formatIntOrUserError() ?: return

        val counter = enteredCrop.getCounter()
        val level = GardenCropMilestones.getTierForCropCount(counter, enteredCrop)
        if (targetLevel <= level && targetLevel != 0) {
            ChatUtils.userError("Custom goal milestone ($targetLevel) must be greater than your current milestone ($level).")
            return
        }
        storage[enteredCrop] = targetLevel
        ChatUtils.chat("Custom goal milestone for §b${enteredCrop.cropName} §eset to §b$targetLevel.")
    }

    fun onComplete(strings: Array<String>): List<String> {
        return if (strings.size <= 1) {
            StringUtils.getListOfStringsMatchingLastWord(
                strings,
                CropType.entries.map { it.simpleName }
            )
        } else listOf()
    }

    private fun Long.formatOutput(needsTime: Boolean, crop: CropType): String {
        if (!needsTime) return "${this.addSeparators()} §a${crop.cropName}"
        val speed = crop.getSpeed() ?: -1
        val missingTime = (this / speed).seconds
        return "${missingTime.format()}§a"
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shcalccrop") {
            description = "Calculate how many crops need to be farmed between different crop milestones."
            category = CommandCategory.USERS_ACTIVE
            autoComplete { onComplete(it) }
            callback { onCommand(it.getOrNull(0), it.getOrNull(1), it.getOrNull(2), false) }
        }
        event.register("shcalccroptime") {
            description = "Calculate how long you need to farm crops between different crop milestones."
            category = CommandCategory.USERS_ACTIVE
            autoComplete { onComplete(it) }
            callback { onCommand(it.getOrNull(0), it.getOrNull(1), it.getOrNull(2), true) }
        }
        event.register("shcropgoal") {
            description = "Define a custom milestone goal for a crop."
            category = CommandCategory.USERS_ACTIVE
            callback { setGoal(it) }
            autoComplete { onComplete(it) }
        }
    }
}
