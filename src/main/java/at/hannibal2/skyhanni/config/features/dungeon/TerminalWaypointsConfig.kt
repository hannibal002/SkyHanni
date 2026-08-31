package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TerminalWaypointsConfig {

    @Expose
    @ConfigOption(name = "Terminal Waypoints", desc = "Displays Waypoints in the F7/M7 Goldor Phase.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color to use for inactive Terminals.")
    @ConfigEditorColour
    var inactiveTerminalColor: ChromaColour = LorenzColor.RED.toChromaColor()

    @Expose
    @ConfigOption(name = "Hide Active Terminals", desc = "whether to remove the Terminals when activated.")
    @ConfigEditorBoolean
    var removeActiveTerminals: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color to use for active Terminals.")
    @ConfigEditorColour
    var activeTerminalColor: ChromaColour = LorenzColor.GREEN.toChromaColor()
}
