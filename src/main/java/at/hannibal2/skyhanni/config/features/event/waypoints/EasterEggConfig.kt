package at.hannibal2.skyhanni.config.features.event.waypoints

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EasterEggConfig {

    @Expose
    @ConfigOption(
        name = "Egg Waypoints",
        desc = "Show all Easter Egg waypoints.\n" +
            "§eCoordinates may not always be up to date!§7\n" +
            "Last updated: 2026",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var allWaypoints: Boolean = false

    @Expose
    @ConfigOption(
        name = "Entrance Waypoints",
        desc = "Not needed for current years Easter Eggs",
    )
    @ConfigEditorBoolean
    var allEntranceWaypoints: Boolean = false

    @Expose
    @ConfigOption(name = "Only Closest", desc = "Only show the closest waypoint.")
    @ConfigEditorBoolean
    var onlyClosest: Boolean = true
}
