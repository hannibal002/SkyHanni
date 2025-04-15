package at.hannibal2.skyhanni.config.features.rift.area.stillgorechateau

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EffigiesConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show locations of inactive Blood Effigies.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Respawning Soon", desc = "Show effigies that are about to respawn.")
    @ConfigEditorBoolean
    @FeatureToggle
    var respawningSoon: Boolean = false

    @Expose
    @ConfigOption(name = "Respawning Time", desc = "Time in minutes before effigies respawn to show.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 15f, minStep = 1f)
    var respawningSoonTime: Int = 3

    @Expose
    @ConfigOption(name = "Unknown Times", desc = "Show effigies without known time.")
    @ConfigEditorBoolean
    @FeatureToggle
    var unknownTime: Boolean = false
}
