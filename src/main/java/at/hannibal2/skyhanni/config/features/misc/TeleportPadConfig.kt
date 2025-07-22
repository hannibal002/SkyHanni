package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TeleportPadConfig {
    @Expose
    @ConfigOption(name = "Compact Name", desc = "Hide the 'Warp to' and 'No Destination' texts over teleport pads.")
    @ConfigEditorBoolean
    @FeatureToggle
    var compactName: Boolean = false

    @Expose
    @ConfigOption(
        name = "Inventory Numbers",
        desc = "Show the number of the teleport pads inside the 'Change Destination' inventory as stack size."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var inventoryNumbers: Boolean = false
}
