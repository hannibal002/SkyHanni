package at.hannibal2.skyhanni.config.features.garden.pests

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class PestFinderConfig {
    @Expose
    @ConfigOption(name = "Display", desc = "Show a display with all known pest locations.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showDisplay: Boolean = true

    @Expose
    @ConfigOption(name = "Show Plot in World", desc = "Mark infested plot names and world border in the world.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showPlotInWorld: Boolean = true

    @Expose
    @ConfigOption(name = "Plot Visibility Type", desc = "Choose how to show infested plots in the world.")
    @ConfigEditorDropdown
    var visibilityType: VisibilityType = VisibilityType.BOTH

    enum class VisibilityType(private val displayName: String) {
        BORDER("Border"),
        NAME("Name"),
        BOTH("Both"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Only With Vacuum",
        desc = "Only show the pest display and waypoints while holding a vacuum in the hand."
    )
    @ConfigEditorBoolean
    var onlyWithVacuum: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show For Seconds",
        desc = "Show plots border for a given amount of seconds after holding a vacuum.\n" +
            "§e0 = Always show when holding vacuum"
    )
    @ConfigEditorSlider(minStep = 1f, minValue = 0f, maxValue = 10f)
    var showBorderForSeconds: Int = 1

    @Expose
    @ConfigLink(owner = PestFinderConfig::class, field = "showDisplay")
    var position: Position = Position(-350, 200, 1.3f)

    @Expose
    @ConfigOption(
        name = "No Pests Title",
        desc = "Show a Title in case of No pests. Useful if you are using the §eGarden Pest Chat Filter"
    )
    @ConfigEditorBoolean
    var noPestTitle: Boolean = false

    @Expose
    @ConfigOption(name = "Teleport Hotkey", desc = "Press this key to warp to the nearest plot with pests on it.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var teleportHotkey: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Always Teleport",
        desc = "Allow teleporting with the Teleport Hotkey even when you're already in an infested plot."
    )
    @ConfigEditorBoolean
    var alwaysTp: Boolean = false

    @Expose
    @ConfigOption(
        name = "Back to Garden",
        desc = "Make the Teleport Hotkey warp you to Garden if you don't have any pests."
    )
    @ConfigEditorBoolean
    var backToGarden: Boolean = false
}
