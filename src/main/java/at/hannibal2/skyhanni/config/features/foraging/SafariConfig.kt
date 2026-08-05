package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SafariConfig {

    @Expose
    @ConfigOption(
        name = "Names in Center",
        desc = "Shows the names of the 4 areas while in the center of the Critter Safari.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var namesInCenter: Boolean = false

    @Expose
    @ConfigOption(
        name = "Hideyho Finder",
        desc = "Helps you find where Hideyho is hiding.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideyhoFinder: Boolean = true

}
