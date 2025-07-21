package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PetCandyDisplayConfig {
    @Expose
    @ConfigOption(name = "Pet Candy Used", desc = "Show the number of Pet Candy used on a pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showCandy: Boolean = true

    @Expose
    @ConfigOption(name = "Hide On Maxed", desc = "Hide the candy count on pets that are max level.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideOnMaxed: Boolean = false
}
