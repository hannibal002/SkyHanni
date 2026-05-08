package at.hannibal2.skyhanni.config.features.garden.greenhouse

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SkyMutationsImportConfig {

    @Expose
    @ConfigOption(name = "Display Type", desc = "Change the way the layout is displayed.")
    @ConfigEditorDropdown
    var displayType: LayoutDisplayType = LayoutDisplayType.ALL

    @Expose
    @ConfigOption(name = "Enabled", desc = "Displays the imported layout inside your greenhouse.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    enum class LayoutDisplayType(val displayName: String) {
        ALL("§aAll"),
        CROPS("§6Crops"),
        SURFACE("§dSurfaces"),
    }
}
