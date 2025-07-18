package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PatcherCoordsWaypointConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Highlight the coordinates sent by Patcher.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Color", desc = "Color of the waypoint.")
    @ConfigEditorColour
    var color: String = "0:194:75:197:64"

    @Expose
    @ConfigOption(name = "Duration", desc = "Duration of the waypoint.")
    @ConfigEditorSlider(minStep = 5f, maxValue = 120f, minValue = 1f)
    var duration: Int = 60
}
