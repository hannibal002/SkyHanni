package at.hannibal2.skyhanni.config.features.misc.navigation

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class NavigationConfig {
    @Expose
    @ConfigOption(name = "Allow Instant Navigate", desc = "Starts navigating instantly upon shnavigate with only one match.")
    @ConfigEditorBoolean
    val allowInstaNavigate: Boolean = true

    @Expose
    @ConfigOption(name = "Area Path Finder", desc = "Helps navigate to different areas on the current island.")
    @Accordion
    val areaPathfinder: AreaPathfinderConfig = AreaPathfinderConfig()

    @Expose
    @ConfigOption(name = "Island Areas", desc = "Settings for Island Areas.")
    @Accordion
    val islandAreas: IslandAreasConfig = IslandAreasConfig()

    @Expose
    @ConfigOption(name = "Pathfinding", desc = "General settings for Pathfinding/Navigating in different features.")
    @Accordion
    val pathfinding: PathfindConfig = PathfindConfig()
}
