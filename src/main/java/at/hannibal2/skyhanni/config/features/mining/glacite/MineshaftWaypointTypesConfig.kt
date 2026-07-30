package at.hannibal2.skyhanni.config.features.mining.glacite

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MineshaftWaypointTypesConfig {
    @Expose
    @ConfigOption(
        name = "Entrance",
        desc = "Mark the location of the entrance with a waypoint."
    )
    @ConfigEditorBoolean
    var entrance: Boolean = true

    @Expose
    @ConfigOption(
        name = "Ladder",
        desc = "Mark the location of the ladders at the bottom of the entrance with a waypoint."
    )
    @ConfigEditorBoolean
    var ladder: Boolean = false

    @Expose
    @ConfigOption(
        name = "Found Corpse",
        desc = "Mark the location of corpses that have been within line of sight with a waypoint.\n" +
            "§eFound Corpse waypoints are replaced by Looted Corpse waypoints when looted!"
    )
    @ConfigEditorBoolean
    var foundCorpse: Boolean = true

    @Expose
    @ConfigOption(
        name = "Looted Corpse",
        desc = "Mark the location of corpses that have been looted."
    )
    @ConfigEditorBoolean
    var lootedCorpse: Boolean = true
}
