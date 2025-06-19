package at.hannibal2.skyhanni.config.features.crimsonisle

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MatriarchHelperConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable features around the Matriarch helper.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Highlight", desc = "Highlight the pearls in a color of your choosing.")
    @ConfigEditorBoolean
    var highlight: Boolean = true

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "Color the pearls are highlighted in.")
    @ConfigEditorColour
    var highlightColor: String = "0:114:126:255:41"

    @Expose
    @ConfigOption(name = "Draw Line", desc = "Draw a line to the lowest Heavy Pearl. Further settings below.")
    @ConfigEditorBoolean
    var line: Boolean = true

    @Expose
    @ConfigOption(name = "Simple Line", desc = "Only draws the line between the Heavy Pearls and not to the exit.")
    @ConfigEditorBoolean
    var simpleLine: Boolean = false

    @Expose
    @ConfigOption(
        name = "Shortest Distance",
        desc = "Uses the shortest Distance instead of height for ordering the pearls. §cNot recommended!",
    )
    @ConfigEditorBoolean
    var useShortestDistance: Boolean = false

    @Expose
    @ConfigOption(name = "Line Color", desc = "Color of the line.")
    @ConfigEditorColour
    var lineColor: String = "0:230:163:38:255"
}
