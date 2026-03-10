package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
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
    @SearchTag("fig mangrove")
    var cleanView = true

    @Expose
    @ConfigOption(name = "Tree Progress Display", desc = "")
    @Accordion
    val progress = TreeProgressConfig()

    @Expose
    @ConfigOption(name = "Compact Sweep Details", desc = "Compacts messages related to Sweep Details calculations.")
    @ConfigEditorBoolean
    @FeatureToggle
    var compactSweepDetails = true

    @Expose
    @ConfigOption(name = "Mute Tree Breaking", desc = "Mutes the sound of the tree fully breaking.")
    @ConfigEditorBoolean
    @FeatureToggle
    var muteBreaking = true

    @Expose
    @ConfigOption(name = "Also on Galatea", desc = "Also mutes tree breaking sounds on Galatea.")
    @ConfigEditorBoolean
    var muteBreakingOnGalatea = false

}
