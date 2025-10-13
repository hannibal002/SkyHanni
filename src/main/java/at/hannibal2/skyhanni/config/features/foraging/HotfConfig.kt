package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.hotx.CurrencyPerHotxPerk.CurrencySpentDesign
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HotfConfig {
    @Expose
    @ConfigOption(
        name = "Enable Highlight",
        desc = "Highlight enabled perks in the HOTF tree §agreen§7, and disabled §cred§7. Locked perks are highlighted gray.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightEnabledPerks: Boolean = true

    @Expose
    @ConfigOption(name = "Lottery Display", desc = "Display your current Lottery perk in a GUI element.")
    @ConfigEditorBoolean
    @FeatureToggle
    var lotteryDisplay: Boolean = false

    @Expose
    @ConfigLink(owner = HotfConfig::class, field = "lotteryDisplay")
    val lotteryPosition: Position = Position(100, 100)

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
    @ConfigOption(name = "Whispers Spent", desc = "Show the amount of forest whispers spent on a perk.")
    @ConfigEditorBoolean
    @FeatureToggle
    var whispersSpent: Boolean = true

    @Expose
    @ConfigOption(name = "Whispers Spent Design", desc = "Change the design of the whispers spent display.")
    @ConfigEditorDropdown
    var whispersSpentDesign: CurrencySpentDesign = CurrencySpentDesign.NUMBER_AND_PERCENTAGE

    @Expose
    @ConfigOption(
        name = "Whispers for 10 Levels",
        desc = "Show the amount of forest whispers needed to level a perk up 10 times when holding the shift key."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var whispersFor10Levels: Boolean = true

    @Expose
    @ConfigOption(
        name = "Current Whispers",
        desc = "Displays the current amount of whispers available for the specific perk when viewing its tooltip."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var currentWhispers: Boolean = true
}
