package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
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
}
