package at.hannibal2.skyhanni.config.features.pets

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PetExperienceToolTipConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show the progress to level 100 (ignoring rarity) when hovering over a pet while pressing shift key."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var petDisplay: Boolean = true

    @Expose
    @ConfigOption(name = "Show Always", desc = "Show this info always, even if not pressing shift key.")
    @ConfigEditorBoolean
    var showAlways: Boolean = false

    @Expose
    @ConfigOption(name = "Dragon Egg", desc = "For a Golden Dragon Egg, show progress to level 100 instead of 200.")
    @ConfigEditorBoolean
    var showGoldenDragonEgg: Boolean = true
}
