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
            "Coordinates by §bL3Cache§7. (last updated: 2025)",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var allWaypoints: Boolean = false

    @Expose
    @ConfigOption(
        name = "Entrance Waypoints",
        desc = "Show helper waypoints to Baskets #18, #27, and #30.\n" +
            "Coordinates by §bSorkoPiko§7 and §bErymanthus§7.",
    )
    @ConfigEditorBoolean
    var allEntranceWaypoints: Boolean = false

    @Expose
    @ConfigOption(name = "Only Closest", desc = "Only show the closest waypoint.")
    @ConfigEditorBoolean
    var onlyClosest: Boolean = true
}
