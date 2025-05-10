package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AtmosphericFilterDisplayConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Toggle the Atmospheric Filter display to show the currently active buff.\n" +
            "§eNote: For an optimal experience, please have the Atmospheric Filter accessory active."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Only Show Buff",
        desc = "Show only the currently active buff without the currently active season."
    )
    @ConfigEditorBoolean
    var onlyBuff: Boolean = false

    @Expose
    @ConfigOption(name = "Abbreviate Season", desc = "Abbreviate the current season.")
    @ConfigEditorBoolean
    var abbreviateSeason: Boolean = false

    @Expose
    @ConfigOption(name = "Abbreviate Perk", desc = "Abbreviate the currently active buff.")
    @ConfigEditorBoolean
    var abbreviatePerk: Boolean = false

    @Expose
    @ConfigOption(name = "Outside Garden", desc = "Show this HUD everywhere, including outside of the Garden.")
    @ConfigEditorBoolean
    var outsideGarden: Boolean = false

    @Expose
    @ConfigLink(owner = AtmosphericFilterDisplayConfig::class, field = "enabled")
    val position: Position = Position(10, 10, true)

    @Expose
    @ConfigLink(owner = AtmosphericFilterDisplayConfig::class, field = "outsideGarden")
    val positionOutside: Position = Position(20, 20, true)
}
