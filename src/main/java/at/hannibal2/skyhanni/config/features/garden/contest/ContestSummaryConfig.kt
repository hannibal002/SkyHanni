package at.hannibal2.skyhanni.config.features.garden.contest

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ContestSummaryConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show the average Blocks Per Second and blocks clicked at the end of a Jacob Farming Contest in chat."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Hide 0-Crop Stats", desc = "Do not display stats for events with 0 crops broken. Useful for pest farming.")
    @ConfigEditorBoolean
    var hideZeroCropStats: Boolean = true
}
