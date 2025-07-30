package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class RngMeterConfig {
    @Expose
    @ConfigOption(name = "Floor Names", desc = "Show the Floor names in the Catacombs RNG Meter inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    var floorName: Boolean = false

    @Expose
    @ConfigOption(
        name = "No Drop",
        desc = "Highlight floors without a drop selected in the Catacombs RNG Meter inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var noDrop: Boolean = false

    @Expose
    @ConfigOption(
        name = "Selected Drop",
        desc = "Highlight the selected drop in the Catacombs or Slayer RNG Meter inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var selectedDrop: Boolean = false
}
