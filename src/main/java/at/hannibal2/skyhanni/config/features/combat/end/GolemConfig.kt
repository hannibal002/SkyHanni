package at.hannibal2.skyhanni.config.features.combat.end

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GolemConfig {
    @Expose
    @ConfigOption(
        name = "Weight Message",
        desc = "Shows your End Stone Protector weight in chat after it died."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var weightChat: Boolean = false

    @Expose
    @ConfigOption(
        name = "Rare Drop Alert",
        desc = "Shows a large title when a rare End Stone Protector drop appears."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var dropAlert: Boolean = false

    @Expose
    @ConfigOption(
        name = "Protector HUD",
        desc = "One framed overlay showing everything about the End Stone Protector."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var display: Boolean = false

    @Expose
    @ConfigLink(owner = GolemConfig::class, field = "display")
    val displayPosition: Position = Position(120, 190)

    @Expose
    @ConfigOption(name = "Show Stage", desc = "Show how far the protector has awoken, as a number.")
    @ConfigEditorBoolean
    var showStage: Boolean = true

    @Expose
    @ConfigOption(name = "Show Location", desc = "Show at which fixed point the protector spawns.")
    @ConfigEditorBoolean
    var showLocation: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Spawn Countdown",
        desc = "Count down the 21 seconds until the protector can be attacked."
    )
    @ConfigEditorBoolean
    var showSpawnTimer: Boolean = true

    @Expose
    @ConfigOption(name = "Show Damage", desc = "Show the damage you dealt to the protector so far.")
    @ConfigEditorBoolean
    var showDamage: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Zealots",
        desc = "Show how many zealots and zealot bruisers you killed on this server. " +
            "Resets when you switch servers. " +
            "§eEnable the Bestiary widget in your tab list for an exact count. Without it, or once your " +
            "Zealot bestiary is maxed, only zealots you hit or aim at are counted."
    )
    @ConfigEditorBoolean
    var showZealots: Boolean = true

    @Expose
    @ConfigOption(
        name = "Late Stage Title",
        desc = "Shows the stage in the middle of the screen when you join a server where the " +
            "protector is already awakening or summoned."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var joinStageTitle: Boolean = false

    @Expose
    @ConfigOption(
        name = "Skyhanni Prefix",
        desc = "Displays the Skyhanni prefix in the protector weight message."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var skyhanniMessagePrefix: Boolean = true

    @Expose
    @ConfigOption(
        name = "Highlight Spawn Points",
        desc = "Marks all six fixed spawn points in the world with a beam on the head structure " +
            "and a highlighted block where the protector appears."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightSpawnPoints: Boolean = false

    @Expose
    @ConfigOption(name = "Spawn Point Color", desc = "Color of the marked spawn point.")
    @ConfigEditorColour
    var activeSpawnColor: ChromaColour = ChromaColour.fromStaticRGB(255, 85, 255, 100)
}
