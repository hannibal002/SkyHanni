package at.hannibal2.skyhanni.config.features.combat.mobsconfig

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MobsConfig {
    @Expose
    @ConfigOption(name = "Arachne Options", desc = "")
    @Accordion
    val arachneSettings: ArachneConfig = ArachneConfig()

    // TODO Separate all of these Into Accordions for each Mob.

    @Expose
    @ConfigOption(
        name = "Area Boss Highlighter",
        desc = "Highlight Golden Ghoul, Old Wolf, Voidling Extremist, Millenia-Aged Blaze and Soul of the Alpha.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var areaBossHighlight: Boolean = true

    @Expose
    @ConfigOption(name = "Corleone", desc = "Highlight Boss Corleone in the Crystal Hollows.")
    @ConfigEditorBoolean
    @FeatureToggle
    var corleoneHighlighter: Boolean = true

    @Expose
    @ConfigOption(name = "Zealot", desc = "Highlight Zealots and Bruisers in The End.")
    @ConfigEditorBoolean
    @FeatureToggle
    var zealotBruiserHighlighter: Boolean = false

    @Expose
    @ConfigOption(name = "Zealot with Chest", desc = "Highlight Zealots holding a Chest in a different color.")
    @ConfigEditorBoolean
    @FeatureToggle
    var chestZealotHighlighter: Boolean = false

    @Expose
    @ConfigOption(
        name = "Special Zealots",
        desc = "Highlight Special Zealots (the ones that drop Summoning Eyes) in the End.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var specialZealotHighlighter: Boolean = true

    @Expose
    @ConfigOption(name = "Corrupted Mob", desc = "Highlight corrupted mobs in purple color.")
    @ConfigEditorBoolean
    @FeatureToggle
    var corruptedMobHighlight: Boolean = false

    @Expose
    @ConfigOption(name = "Runic Mob", desc = "Highlight runic mobs in light purple color.")
    @ConfigEditorBoolean
    @FeatureToggle
    var runicMobHighlight: Boolean = false

    @Expose
    @ConfigOption(
        name = "Area Boss Timer",
        desc = "Show a timer when Area Bosses respawn.\n" +
            "§eMay take 20 - 30 seconds to calibrate correctly.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var areaBossRespawnTimer: Boolean = false

    @Expose
    @ConfigOption(name = "Enderman TP Hider", desc = "Stops the Enderman Teleportation animation.")
    @ConfigEditorBoolean
    @FeatureToggle
    var endermanTeleportationHider: Boolean = true
}
