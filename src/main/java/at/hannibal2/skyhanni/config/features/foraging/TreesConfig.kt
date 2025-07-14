package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class TreesConfig {

    @Expose
    @ConfigOption(name = "Clean Tree View", desc = "Hides the floating blocks when mining trees in galatea.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    @SearchTag("fig mangrove")
    var cleanView = true

    @Expose
    @ConfigOption(name = "Tree Progress Display", desc = "")
    @OnlyModern
    @Accordion
    val progress = TreeProgressConfig()

    @Expose
    @ConfigOption(name = "Compact Sweep Details", desc = "Compacts messages related to Sweep Details calculations.")
    @ConfigEditorBoolean
    @FeatureToggle
    var compactSweepDetails = true

}
