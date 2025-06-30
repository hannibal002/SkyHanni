package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyLegacy
import at.hannibal2.skyhanni.config.OnlyModern
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

/**
 * Attention developers:
 * If your feature can only be used on the foraging islands please mark it with @[OnlyModern]
 */
class ForagingConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "To see all foraging features, please launch the game on a modern version of Minecraft with SkyHanni installed.\n" +
            "§eJoin the SkyHanni discord for a guide on how to migrate the config.",
    )
    @OnlyLegacy
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @Category(name = "HotF", desc = "Settings for Heart of the Forest.")
    val hotf: HotfConfig = HotfConfig()

    @Expose
    @Category(name = "Trees", desc = "Settings for big trees found on the foraging islands.")
    val trees: TreesConfig = TreesConfig()

    @Expose
    @ConfigOption(name = "Foraging Tutorial Quest", desc = "")
    @Accordion
    @OnlyModern
    val tutorialQuest: ForagingTutorialQuestConfig = ForagingTutorialQuestConfig()

    @Expose
    @ConfigOption(name = "Moonglade Beacon", desc = "Settings for the moonglade beacon.")
    @OnlyModern
    @Accordion
    var moongladeBeacon = MoongladeBeaconConfig()

    @Expose
    @ConfigOption(name = "Birries Highlight", desc = "")
    @OnlyModern
    @Accordion
    var birriesHighlight = BirriesHighlightConfig()

    @Expose
    @ConfigOption(name = "Hideonleaf Highlight", desc = "")
    @OnlyModern
    @Accordion
    var hideonleafHighlight = HideonleafHighlightConfig()

    @Expose
    @ConfigOption(name = "Foraging Tracker", desc = "")
    @OnlyModern
    @Accordion
    val tracker = ForagingTrackerConfig()

    @Expose
    @ConfigOption(name = "Lasso Display", desc = "Displays your lasso progress on screen.")
    @ConfigEditorBoolean
    @FeatureToggle
    var lassoDisplay = true

    @Expose
    @ConfigOption(name = "Mute Phantoms", desc = "Silences Phantoms in the Galatea.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    var mutePhantoms = true

    @Expose
    @ConfigLink(owner = ForagingConfig::class, field = "lassoDisplay")
    val lassoDisplayPosition: Position = Position(380, 210)

}
