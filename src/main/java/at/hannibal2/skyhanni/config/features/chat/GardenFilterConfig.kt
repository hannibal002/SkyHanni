package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GardenFilterConfig {

    @Expose
    @ConfigOption(
        name = "Anita's Accessories",
        desc = "Hide Anita's Accessories' fortune bonus messages outside the Garden.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideJacob: Boolean = true

    @Expose
    @ConfigOption(name = "Garden Pest", desc = "Hide the message of no pests on garden.")
    @ConfigEditorBoolean
    @FeatureToggle
    var gardenNoPest: Boolean = false

    @Expose
    @ConfigOption(name = "Jacob Contest Start", desc = "Hides Jacob Contest Start messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    var jacobStart: Boolean = false

    @Expose
    @ConfigOption(
        name = "Sack Change Hider",
        desc = "Hide the sack change message while allowing mods to continue accessing sack data.\n" +
            "§eUse this instead of the toggle in Hypixel Settings.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideSacksChange: Boolean = false

    @Expose
    @ConfigOption(
        name = "Only Hide on Garden",
        desc = "Only hide the sack change message in the Garden.",
    )
    @ConfigEditorBoolean
    var onlyHideSacksChangeOnGarden: Boolean = false
}
