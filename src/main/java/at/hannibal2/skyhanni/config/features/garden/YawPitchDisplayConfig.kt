package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class YawPitchDisplayConfig {
    @Expose
    @ConfigOption(
        name = "Enable",
        desc = "Display yaw and pitch while holding a farming tool. Automatically fades out if there is no movement."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Yaw Precision", desc = "Yaw precision up to specified decimal.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var yawPrecision: Int = 4

    @Expose
    @ConfigOption(name = "Pitch Precision", desc = "Pitch precision up to specified decimal.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var pitchPrecision: Int = 4

    @Expose
    @ConfigOption(
        name = "Display Timeout",
        desc = "Duration in seconds for which the overlay is being displayed after moving."
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    var timeout: Int = 5

    @Expose
    @ConfigOption(name = "Show Without Tool", desc = "Does not require you to hold a tool for the overlay to show.")
    @ConfigEditorBoolean
    var showWithoutTool: Boolean = false

    @Expose
    @ConfigOption(name = "Show Outside Garden", desc = "The overlay will work outside of the Garden.")
    @ConfigEditorBoolean
    var showOutsideGarden: Boolean = false

    @Expose
    @ConfigOption(name = "Ignore Timeout", desc = "Ignore the timeout after not moving mouse.")
    @ConfigEditorBoolean
    var showAlways: Boolean = false

    // Todo rename to position
    @Expose
    @ConfigLink(owner = YawPitchDisplayConfig::class, field = "enabled")
    val pos: Position = Position(445, 225)

    // Todo rename to positionOutside
    @Expose
    @ConfigLink(owner = YawPitchDisplayConfig::class, field = "showOutsideGarden")
    val posOutside: Position = Position(445, 225)
}
