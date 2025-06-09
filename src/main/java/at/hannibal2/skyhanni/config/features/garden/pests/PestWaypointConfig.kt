package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PestWaypointConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a waypoint of the next pest when using a vacuum." +
            "Uses the particles and math to detect the location from everywhere in the garden.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Hide Particles", desc = "Hide the particles of the ability.")
    @ConfigEditorBoolean
    var hideParticles: Boolean = true

    @Expose
    @ConfigOption(name = "Draw Line", desc = "Draw a line to the waypoint.")
    @ConfigEditorBoolean
    var drawLine: Boolean = true

    @Expose
    @ConfigOption(name = "Differentiate Plot Middle", desc = "Distinguish pest guesses that point to the middle of the plot")
    @ConfigEditorBoolean
    var differentiatePlotMiddle: Boolean = true

    @Expose
    @ConfigOption(name = "Show For Seconds", desc = "The waypoint will disappear after this number of seconds.")
    @ConfigEditorSlider(minValue = 5f, maxValue = 20f, minStep = 1f)
    var showForSeconds: Int = 15
}
