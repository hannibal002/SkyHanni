package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CleanTreeViewConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Hides floating blocks when foraging trees in Galatea.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled = true

    @Expose
    @ConfigOption(
        name = "Hide Breaking Tree Blocks",
        desc = "Hides floating blocks when breaking tree blocks."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideTreeBlocks = true

    @Expose
    @ConfigOption(
        name = "Hide Rune Effects",
        desc = "Hides other Non Default Floating Tree blocks e.g. Barkshatter Rune's effect."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideRuneEffects = false
}
