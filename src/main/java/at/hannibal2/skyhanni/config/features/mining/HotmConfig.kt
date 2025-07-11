package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.mining.PowderPerHotmPerk.PowderSpentDesign
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

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
    @ConfigOption(name = "Sky Mall Display", desc = "Display your current Sky Mall perk in a GUI element.")
    @ConfigEditorBoolean
    @FeatureToggle
    @SearchTag("skymall")
    var skyMallDisplay: Boolean = false

    @Expose
    @ConfigLink(owner = HotmConfig::class, field = "skyMallDisplay")
    val skyMallPosition: Position = Position(100, 100)

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

    @Expose
    @ConfigOption(name = "Powder Spent Design", desc = "Change the design of the powder spent display.")
    @ConfigEditorDropdown
    var powderSpentDesign: PowderSpentDesign = PowderSpentDesign.NUMBER_AND_PERCENTAGE

    @Expose
    @ConfigOption(
        name = "Powder for 10 Levels",
        desc = "Show the amount of powder needed to level a perk up 10 times when holding the shift key."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var powderFor10Levels: Boolean = true

    @Expose
    @ConfigOption(
        name = "Current Powder",
        desc = "Displays the current amount of powder available for the specific perk when viewing its tooltip."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var currentPowder: Boolean = true
}
