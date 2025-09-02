package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.milestoneNextTierAmount
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.milestoneProgressToNextTier
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.milestoneTotalCropsForTier
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
            val nextTierAmount = crop.milestoneNextTierAmount()
            val progressToNextTier = crop.milestoneProgressToNextTier()
            if (nextTierAmount != null && progressToNextTier != null) {
                val output = (nextTierAmount - progressToNextTier).formatOutput(needsTime, crop)
                ChatUtils.chat("§7$output needed to reach the next milestone")
            } else {
                ChatUtils.userError(
                    "No crop milestone data detected! Please do /cropmilestones and rerun the command," +
                        "or specify your tier targets!"
                )
            }
            return
        }

        if (target == null) {
            val cropsForTier = crop.milestoneTotalCropsForTier(current)
            val output = cropsForTier.formatOutput(needsTime, crop)
            ChatUtils.chat("§7$output needed for milestone §7$current")
            return
        }

        if (current >= target) {
            ChatUtils.userError("Entered milestone is greater than or the same as target milestone")
            return
        }

        val currentAmount = crop.milestoneTotalCropsForTier(current)
        val targetAmount = crop.milestoneTotalCropsForTier(target)
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
            aliases = listOf("shcalcrop")
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
            aliases = listOf("shcalcroptime")
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
    }
}
