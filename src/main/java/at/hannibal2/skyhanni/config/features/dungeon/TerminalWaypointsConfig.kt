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
    @ConfigOption(name = "Enabled", desc = "Displays Waypoints in the F7/M7 Goldor Phase For Terminals/Devices/Arrows.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Inactive Color", desc = "Color to use for inactive Terminals/Devices/Arrows.")
    @ConfigEditorColour
    var inactiveColor: ChromaColour = LorenzColor.RED.toChromaColor()

    @Expose
    @ConfigOption(name = "Hide Active Terminals", desc = "whether to remove the Terminals/Devices/Arrows when activated.")
    @ConfigEditorBoolean
    var removeActive: Boolean = true

    @Expose
    @ConfigOption(name = "Active Color", desc = "Color to use for active Terminals/Devices/Arrows.")
    @ConfigEditorColour
    var activeColor: ChromaColour = LorenzColor.GREEN.toChromaColor()
}
