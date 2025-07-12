package at.hannibal2.skyhanni.config.features.event.hoppity

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HoppityWaypointsConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Toggle guess waypoints for Hoppity's Hunt.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Shared Waypoints",
        desc = "Enable being able to share and receive egg waypoints in your lobby."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var shared: Boolean = true

    @Expose
    @ConfigOption(name = "Color", desc = "Color of the waypoint.")
    @ConfigEditorColour
    var color: String = "0:53:46:224:73"

    @Expose
    @ConfigOption(name = "Show Line", desc = "Show a line to the waypoint.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showLine: Boolean = false

    @Expose
    @ConfigOption(name = "Show Path Finder", desc = "Show a pathfind to the next hoppity egg.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showPathFinder: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show All Waypoints",
        desc = "Show all possible egg waypoints for the current lobby. §e" +
            "Only works when you don't have an Egglocator in your inventory."
    )
    @ConfigEditorBoolean
    var showAll: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hide Duplicate Waypoints",
        desc = "Hide egg waypoints you have already found.\n" +
            "§eOnly works when you don't have an Egglocator in your inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideDuplicates: Boolean = false

    @Expose
    @ConfigOption(
        name = "Mark Duplicate Waypoints",
        desc = "Marks egg location waypoints which you have already found in red."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightDuplicates: Boolean = false

    @Expose
    @ConfigOption(name = "Mark Nearby Duplicates", desc = "Always show duplicate egg locations when nearby.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showNearbyDuplicates: Boolean = false

    @Expose
    //#if FORGE
    @ConfigOption(
        name = "Load from NEU PV",
        desc = "Load Hoppity Egg Location data from API when opening the NEU Profile Viewer."
    )
    //#else
    //$$@ConfigOption(
    //$$    name = "Load from SkyBlock PV",
    //$$    desc = "Load Hoppity Egg Location data from API when opening the SkyBlock Profile Viewer mod."
    //$$)
    //#endif
    @ConfigEditorBoolean
    @FeatureToggle
    var loadFromNeuPv: Boolean = true
}
