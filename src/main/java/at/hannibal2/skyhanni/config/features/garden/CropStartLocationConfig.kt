package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CropStartLocationConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Show waypoints for the farm of your current tool in hand.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Crop Location Mode",
        desc = "Whether to show waypoint at start location (set with §e/shcropstartlocation §7) or last farmed location."
    )
    @ConfigEditorDropdown
    var mode: CropLocationMode = CropLocationMode.START

    enum class CropLocationMode(private val displayName: String) {
        START("Start Only"),
        LAST_FARMED("Last Farmed Only"),
        BOTH("Both"),
        ;

        override fun toString() = displayName
    }
}
