package at.hannibal2.skyhanni.config.features.mining.nucleus

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class AreaWallsConfig {
    @Expose
    @ConfigOption(name = "Area Walls", desc = "Show walls between the main areas of the Crystal Hollows.")
    @ConfigEditorBoolean
    @FeatureToggle
    @SearchTag("Area Borders")
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "In Nucleus", desc = "Also show the walls when inside the Nucleus.")
    @ConfigEditorBoolean
    var nucleus: Boolean = false
}
