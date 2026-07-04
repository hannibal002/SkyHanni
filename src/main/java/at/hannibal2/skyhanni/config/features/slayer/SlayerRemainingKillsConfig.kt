package at.hannibal2.skyhanni.config.features.slayer

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SlayerRemainingKillsConfig {
    @Expose
    @ConfigOption(name = "Remaining Kills", desc = "Display the names and remaining amount of mob kills needed until the boss spawns.")
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = false

    @Expose
    @ConfigOption(name = "Display Overkill", desc = "Add Text To Display when going over needed XP to spawn.")
    @ConfigEditorBoolean
    var showOverkill: Boolean = true

    @Expose
    @ConfigOption(name = "Show XP", desc = "Show the expected XP from the mob in the display.")
    @ConfigEditorBoolean
    var includeExpectedXP: Boolean = false

    @Expose
    @ConfigOption(name = "Show Level", desc = "Include the mob Level in the display.")
    @ConfigEditorBoolean
    var includeMobLevel: Boolean = false

    @Expose
    @ConfigOption(name = "Show Health", desc = "Include the mob Health in the display.")
    @ConfigEditorBoolean
    var includeMobHealth: Boolean = false

    @Expose
    @ConfigLink(owner = SlayerRemainingKillsConfig::class, field = "display")
    val remainingKillsPosition: Position = Position(410, 110)
}
