package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class StatsTuningConfig {
    @Expose
    @ConfigOption(name = "Selected Stats", desc = "Show the tuning stats in the Thaumaturgy inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var selectedStats: Boolean = true

    @Expose
    @ConfigOption(
        name = "Tuning Points",
        desc = "Show the amount of selected Tuning Points in the Stats Tuning inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var points: Boolean = true

    @Expose
    @ConfigOption(name = "Selected Template", desc = "Highlight the selected template in the Stats Tuning inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var selectedTemplate: Boolean = true

    @Expose
    @ConfigOption(name = "Template Stats", desc = "Show the type of stats for the Tuning Point templates.")
    @ConfigEditorBoolean
    @FeatureToggle
    var templateStats: Boolean = true
}
