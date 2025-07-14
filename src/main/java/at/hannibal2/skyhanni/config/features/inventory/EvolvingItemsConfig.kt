package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EvolvingItemsConfig {
    @Expose
    @ConfigOption(
        name = "Time Held in Lore",
        desc = "Show time held for Evolving Items (Bottle of Jyrre, Dark Cacao Truffle, etc.) in the lore."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var timeHeldInLore: Boolean = false

    @Expose
    @ConfigOption(
        name = "Time Left in Lore",
        desc = "Show time until maxed for Evolving Items (Bottle of Jyrre, Dark Cacao Truffle, etc.) in the lore."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var timeLeftInLore: Boolean = false
}
