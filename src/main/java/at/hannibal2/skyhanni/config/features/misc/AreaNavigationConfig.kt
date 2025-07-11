package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AreaNavigationConfig {
    @Expose
    @ConfigOption(name = "Area Path Finder", desc = "")
    @Accordion
    val pathfinder: AreaPathfinderConfig = AreaPathfinderConfig()

    @Expose
    @ConfigOption(name = "In World", desc = "Shows the area names in world")
    @ConfigEditorBoolean
    @FeatureToggle
    var inWorld: Boolean = false

    @Expose
    @ConfigOption(name = "Small Areas", desc = "Include small areas.")
    @ConfigEditorBoolean
    var includeSmallAreas: Boolean = false

    @Expose
    @ConfigOption(name = "Title on Enter", desc = "Sends a titles on screen when entering an area.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enterTitle: Boolean = false
}
