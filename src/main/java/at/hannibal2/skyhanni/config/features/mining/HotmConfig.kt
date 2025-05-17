package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
//#if TODO
import at.hannibal2.skyhanni.features.mining.PowderPerHotmPerk.PowderSpentDesign
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class HotmConfig {
    @Expose
    @ConfigOption(
        name = "Enable Highlight",
        desc = "Highlight enabled perks in the HOTM tree §agreen§7, and disabled §cred§7. Locked perks are highlighted gray."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightEnabledPerks: Boolean = true

    @Expose
    @ConfigOption(name = "Level Stack", desc = "Show the level of a perk as item stacks.")
    @ConfigEditorBoolean
    @FeatureToggle
    var levelStackSize: Boolean = true

    @Expose
    @ConfigOption(name = "Token Stack", desc = "Show unused tokens on the heart.")
    @ConfigEditorBoolean
    @FeatureToggle
    var tokenStackSize: Boolean = true

    @Expose
    @ConfigOption(name = "Powder Spent", desc = "Show the amount of powder spent on a perk.")
    @ConfigEditorBoolean
    @FeatureToggle
    var powderSpent: Boolean = true

    //#if TODO
    @Expose
    @ConfigOption(name = "Powder Spent Design", desc = "Change the design of the powder spent display.")
    @ConfigEditorDropdown
    var powderSpentDesign: PowderSpentDesign = PowderSpentDesign.NUMBER_AND_PERCENTAGE
    //#endif

    @Expose
    @ConfigOption(
        name = "Powder for 10 Levels",
        desc = "Show the amount of powder needed to level a perk up 10 times when holding the shift key."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var powderFor10Levels: Boolean = true
}
