package at.hannibal2.skyhanni.config.features.misc.navigation

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class NavigationConfig {

    @Expose
    @ConfigOption(name = "Areas List", desc = "")
    @Accordion
    val areasList: AreasListConfig = AreasListConfig()

    @Expose
    @ConfigOption(name = "Show in World", desc = "Shows neighboring area names as waypoints at their borders.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showInWorld: Boolean = false

    @Expose
    @ConfigOption(name = "Title on Enter", desc = "Sends a title on screen when entering an area.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enterTitle: Boolean = false

    @Expose
    @ConfigOption(name = "Pathfinding", desc = "General settings for Pathfinding/Navigating in different features.")
    @Accordion
    val pathfinding: PathfindConfig = PathfindConfig()

    @Expose
    @ConfigOption(name = "Allow Instant Navigation", desc = "Starts navigating instantly upon §e/shnavigate§r with only one match.")
    @ConfigEditorBoolean
    var allowInstantNavigation: Boolean = true

    @Expose
    @ConfigOption(name = "Small Areas", desc = "Include small areas in the areas list and world display.")
    @ConfigEditorBoolean
    var includeSmallAreas: Boolean = false
}
