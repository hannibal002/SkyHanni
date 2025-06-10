package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class NumbersConfig {
    @Expose
    @ConfigOption(name = "Crop Milestone", desc = "Show the number of crop milestones in the inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var cropMilestone: Boolean = true

    @Expose
    @ConfigOption(name = "Average Milestone", desc = "Show the average crop milestone in the crop milestone inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var averageCropMilestone: Boolean = true

    @Expose
    @ConfigOption(name = "Crop Upgrades", desc = "Show the number of upgrades in the crop upgrades inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var cropUpgrades: Boolean = true

    @Expose
    @ConfigOption(
        name = "Composter Upgrades",
        desc = "Show the number of upgrades in the Composter upgrades inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var composterUpgrades: Boolean = true
}
