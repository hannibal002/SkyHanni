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
        name = "Missing Crop Warning",
        desc = "Scan Greenhouse plots and warn when one or more unique crops are not planted.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var missingCropWarning: Boolean = false

    @Expose
    @ConfigLink(owner = GreenhouseConfig::class, field = "missingCropWarning")
    val cropChecklistPosition: Position = Position(10, 80)

    @Expose
    @ConfigOption(
        name = "Mutation Blueprint",
        desc = "Save and load Greenhouse layouts, then show shadows where mutations disappear. " +
            "Use /shgreenhouseblueprint to open the layout library.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var mutationBlueprint: Boolean = false

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
    @ConfigOption(
        name = "Highlight Water Status",
        desc = "Highlights the \"Water Status\" item green if the crop has enough water.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightWaterStatus: Boolean = true

    @Expose
    @ConfigLink(owner = GreenhouseConfig::class, field = "showDisplay")
    val position: Position = Position(180, 40)

    @Expose
    @ConfigOption(
        name = "Phantomleaf Solver",
        desc = "When harvesting Phantomleaf, highlight the hiding spot in green. §eStand still for best results.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var phantomleafSolver: Boolean = true

    @Expose
    @ConfigOption(
        name = "Sky Mutations Link",
        desc = "Offer a link to §aSky Mutations§7 in the Carpenter inventory in the Greenhouse and in the Crop Analyzer. " +
            "The website offers useful information about mutations.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var mutationsWebsite: Boolean = true
}
