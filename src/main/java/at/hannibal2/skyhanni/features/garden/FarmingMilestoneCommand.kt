package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.TimeUtils.format
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FarmingMilestoneCommand {

    private fun onCommand(crop: CropType, current: Int?, target: Int?, needsTime: Boolean) {
        if (current == null) {
            val currentProgress = crop.getCounter()
            val currentCropMilestone = GardenCropMilestones.getTierForCropCount(currentProgress, crop, allowOverflow = true) + 1
            val cropsForTier = GardenCropMilestones.getCropsForTier(currentCropMilestone, crop, allowOverflow = true)
            val output = (cropsForTier - currentProgress).formatOutput(needsTime, crop)

            ChatUtils.chat("§7$output needed to reach the next milestone")
            return
        }

        if (target == null) {
            val cropsForTier = GardenCropMilestones.getCropsForTier(current, crop, allowOverflow = true)
            val output = cropsForTier.formatOutput(needsTime, crop)

            ChatUtils.chat("§7$output needed for milestone §7$current")
            return
        }

        if (current >= target) {
            ChatUtils.userError("Entered milestone is greater than or the same as target milestone")
            return
        }

        val currentAmount = GardenCropMilestones.getCropsForTier(current, crop, allowOverflow = true)
        val targetAmount = GardenCropMilestones.getCropsForTier(target, crop, allowOverflow = true)
        val output = (targetAmount - currentAmount).formatOutput(needsTime, crop)
        ChatUtils.chat("§7$output needed for milestone §7$current §a-> §7$target")
    }

    private fun Long.formatOutput(needsTime: Boolean, crop: CropType): String {
        if (!needsTime) return "${this.addSeparators()} §a${crop.cropName}"
        val speed = crop.getSpeed() ?: -1
        val missingTime = (this / speed).seconds
        return "${missingTime.format()}§a"
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcalccrop") {
            description = "Calculate how many crops need to be farmed between different crop milestones."
            category = CommandCategory.USERS_ACTIVE
            arg("cropType", EnumArgumentType.custom<CropType>({ it.simpleName })) { crop ->
                arg("current", BrigadierArguments.integer()) { current ->
                    arg("target", BrigadierArguments.integer()) { target ->
                        callback {
                            onCommand(getArg(crop), getArg(current), getArg(target), false)
                        }
                    }
                    callback {
                        onCommand(getArg(crop), getArg(current), null, false)
                    }
                }
                callback {
                    onCommand(getArg(crop), null, null, false)
                }
            }
            simpleCallback {
                ChatUtils.userError("No crop type entered")
            }
        }
        event.registerBrigadier("shcalccroptime") {
            description = "Calculate how long you need to farm crops between different crop milestones."
            category = CommandCategory.USERS_ACTIVE
            arg("cropType", EnumArgumentType.custom<CropType>({ it.simpleName })) { crop ->
                arg("current", BrigadierArguments.integer()) { current ->
                    arg("target", BrigadierArguments.integer()) { target ->
                        callback {
                            onCommand(getArg(crop), getArg(current), getArg(target), true)
                        }
                    }
                    callback {
                        onCommand(getArg(crop), getArg(current), null, true)
                    }
                }
                callback {
                    onCommand(getArg(crop), null, null, true)
                }
            }
            simpleCallback {
                ChatUtils.userError("No crop type entered")
            }
        }
        event.registerBrigadier("shcropgoal") {
            description = "Define a custom milestone goal for a crop."
            category = CommandCategory.USERS_ACTIVE
            arg("crop", EnumArgumentType.custom<CropType>({ it.simpleName })) { cropArg ->
                arg("target", BrigadierArguments.integer()) { targetArg ->
                    callback {
                        val storage = ProfileStorageData.profileSpecific?.garden?.customGoalMilestone ?: return@callback

                        val crop = getArg(cropArg)
                        val targetLevel = getArg(targetArg)

                        val counter = crop.getCounter()
                        val level = GardenCropMilestones.getTierForCropCount(counter, crop)
                        if (targetLevel <= level && targetLevel != 0) {
                            ChatUtils.userError(
                                "Custom goal milestone ($targetLevel) must be greater than your current milestone ($level)."
                            )
                            return@callback
                        }
                        storage[crop] = targetLevel
                        ChatUtils.chat("Custom goal milestone for §b${crop.cropName} §eset to §b$targetLevel.")
                    }
                }
                simpleCallback {
                    ChatUtils.userError("Usage: /shcropgoal <crop name> <target milestone>")
                }
            }
            simpleCallback {
                ChatUtils.userError("Usage: /shcropgoal <crop name> <target milestone>")
            }
        }
    }
}
