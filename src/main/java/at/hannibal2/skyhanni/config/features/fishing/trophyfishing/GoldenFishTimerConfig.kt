package at.hannibal2.skyhanni.config.features.fishing.trophyfishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GoldenFishTimerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the Golden Fish Timer features. This is required for all features.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Display Mode", desc = "Type of display to show for the Golden Fish.")
    @ConfigEditorDropdown
    var displayDesign: DesignFormat = DesignFormat.DETAILED_WITH_ICON

    enum class DesignFormat(private val displayName: String) {
        OFF("Off"),
        COMPACT("Compact"),
        DETAILED("Detailed"),
        DETAILED_WITH_ICON("Detailed + Icon"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Nametag",
        desc = "Show a nametag on the Golden Fish showing how weak it is and when it will despawn."
    )
    @ConfigEditorBoolean
    var nametag: Boolean = true

    @Expose
    @ConfigOption(name = "Highlight when ready", desc = "Highlight the Golden Fish when it is ready to be caught.")
    @ConfigEditorBoolean
    var highlight: Boolean = true

    @Expose
    @ConfigOption(
        name = "Throw Rod Warning",
        desc = "Show a warning when you are close to the time limit of throwing your rod."
    )
    @ConfigEditorBoolean
    var throwRodWarning: Boolean = false

    @Expose
    @ConfigOption(name = "Throw Rod Warning Time", desc = "The time in seconds before the throw rod warning appears.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 60f, minStep = 1f)
    var throwRodWarningTime: Int = 20

    @Expose
    @ConfigLink(owner = GoldenFishTimerConfig::class, field = "enabled")
    val position: Position = Position(50, 80)
}
