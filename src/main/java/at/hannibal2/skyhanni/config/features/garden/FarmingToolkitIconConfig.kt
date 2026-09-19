package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FarmingToolkitIconConfig {
    @Expose
    @ConfigOption(name = "Replace Menu Icons", desc = "Show crops instead of tools in the toolkit menu.")
    @ConfigEditorBoolean
    @FeatureToggle
    var replaceMenuIcons: Boolean = true
}
