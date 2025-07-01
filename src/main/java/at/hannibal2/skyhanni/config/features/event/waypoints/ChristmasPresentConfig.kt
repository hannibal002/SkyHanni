package at.hannibal2.skyhanni.config.features.event.waypoints

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ChristmasPresentConfig {
    @Expose
    @ConfigOption(name = "Present Waypoints", desc = "Show all Present waypoints.")
    @ConfigEditorBoolean
    @FeatureToggle
    var allWaypoints: Boolean = false

    // TODO confirm if this toggle actually does anything, ar there helper waypoints at all?
    @Expose
    @ConfigOption(name = "Entrance Waypoints", desc = "Show helper waypoints.")
    @ConfigEditorBoolean
    var allEntranceWaypoints: Boolean = false

    @Expose
    @ConfigOption(name = "Only Closest", desc = "Only show the closest waypoint.")
    @ConfigEditorBoolean
    var onlyClosest: Boolean = false
}
