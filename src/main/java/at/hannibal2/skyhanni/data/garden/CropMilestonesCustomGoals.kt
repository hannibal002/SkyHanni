package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.garden.CropMilestones.config
import at.hannibal2.skyhanni.data.garden.CropMilestones.milestoneTotalCropsForTier
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.farming.GardenCropMilestoneDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import io.github.notenoughupdates.moulconfig.observer.Property

@SkyHanniModule
object CropMilestonesCustomGoals {

    data class MilestoneGoal(val tier: Int, val cropAmount: Long)

    val config get() = CropMilestones.config
    var milestoneCustomGoals: MutableMap<CropType, MilestoneGoal> = mutableMapOf()

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.customGoalCrops.afterChange {
            milestoneCustomGoals.clear()
            for (crop in this) {
                milestoneCustomGoals[crop] = crop.customGoalFromConfig()
                ChatUtils.debug("Set custom goal: ${milestoneCustomGoals[crop]}")
            }
            GardenCropMilestoneDisplay.update()
            ChatUtils.debug("$milestoneCustomGoals")
            ChatUtils.debug("Updated All Custom Goals!")
        }

        for (crop in CropType.entries) {
            ConditionalUtils.onToggle(crop.getCustomGoalConfig()) {
                milestoneCustomGoals.replace(crop, crop.customGoalFromConfig())
                GardenCropMilestoneDisplay.update()
                ChatUtils.debug("Custom goal $crop set")
            }
        }
    }

    fun CropType.getCustomGoal() = milestoneCustomGoals[this]

    fun loadCustomGoals() {
        for (crop in config.customGoalCrops.get()) {
            milestoneCustomGoals[crop] = crop.customGoalFromConfig()
            GardenCropMilestoneDisplay.update()
        }
    }

    private fun CropType.customGoalFromConfig(): MilestoneGoal {
        val customGoalTier = this.getCustomGoalConfig().get().toInt()
        val customGoalAmount = this.milestoneTotalCropsForTier(customGoalTier) ?: 0
        return MilestoneGoal(customGoalTier, customGoalAmount)
    }

    private fun CropType.getCustomGoalConfig(): Property<Float> = with(config.customGoalConfig) {
        when (this@getCustomGoalConfig) {
            CropType.WHEAT -> wheat
            CropType.CARROT -> carrot
            CropType.POTATO -> potato
            CropType.NETHER_WART -> wart
            CropType.PUMPKIN -> pumpkin
            CropType.MELON -> melon
            CropType.COCOA_BEANS -> cocoa
            CropType.SUGAR_CANE -> cane
            CropType.CACTUS -> cactus
            CropType.MUSHROOM -> mushroom
        }
    }
}
