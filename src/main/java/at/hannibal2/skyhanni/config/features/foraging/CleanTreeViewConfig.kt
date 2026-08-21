package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CleanTreeViewConfig {
    @Expose
    @ConfigOption(
        name = "Hide Block Break Effects",
        desc = "Hides floating blocks created when breaking tree blocks."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideTreeBlocks = true

    @Expose
    @ConfigOption(
        name = "Hide Rune Effects",
        desc = "Hides floating blocks created by rune effects, such as Barkshatter."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideRuneEffects = false
}
