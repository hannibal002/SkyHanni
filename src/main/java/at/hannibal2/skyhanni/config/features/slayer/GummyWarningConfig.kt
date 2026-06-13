package at.hannibal2.skyhanni.config.features.slayer

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GummyWarningConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Send a warning when you don't have Smoldering Polarization active.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Only When Slayer Active", desc = "Only warn when you have an active slayer quest.")
    @ConfigEditorBoolean
    var onlyWhenSlayerActive: Boolean = true

    @Expose
    @ConfigOption(name = "Only With Habanero Tactics", desc = "Only warn if you're wearing armor with Habanero Tactics.")
    @ConfigEditorBoolean
    var onlyWithHabanero: Boolean = true
}
