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
        desc = "Count down the 20 seconds until the protector can be attacked."
    )
    @ConfigEditorBoolean
    var showSpawnTimer: Boolean = true

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
