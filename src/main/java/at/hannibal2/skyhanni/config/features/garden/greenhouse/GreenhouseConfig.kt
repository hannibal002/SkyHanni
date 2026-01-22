package at.hannibal2.skyhanni.config.features.garden.greenhouse

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GreenhouseConfig {

    @Expose
    @ConfigOption(
        name = "Growth Cycle Timer",
        desc = "Show a timer for the next growth stage. Open the Crop Diagnostics menu in the Greenhouse to detect the time.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showDisplay: Boolean = true

    @Expose
    @ConfigOption(
        name = "Only Show When Ready",
        desc = "Only show the timer when it is ready.",
    )
    @ConfigEditorBoolean
    var onlyShowWhenOverdue: Boolean = false

    @Expose
    @ConfigOption(
        name = "Highlight Harvestable Status",
        desc = "Highlights the \"Growth Status\" beacon green if the crop is harvestable and red if it is not.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightHarvestableStatus: Boolean = true

    @Expose
    @ConfigLink(owner = GreenhouseConfig::class, field = "showDisplay")
    val position: Position = Position(180, 40)
}
