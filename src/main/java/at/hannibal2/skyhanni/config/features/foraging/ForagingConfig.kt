package at.hannibal2.skyhanni.config.features.foraging

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.OnlyLegacy
import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

/**
 * Attention developers:
 * If your feature can only be used on the foraging islands please mark it with @[OnlyModern]
 */
class ForagingConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "To see all Foraging features, please launch the game on a modern version of Minecraft with SkyHanni installed.\n" +
            "§eJoin the SkyHanni Discord for a guide on how to migrate the config.",
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
    @ConfigOption(name = "Starlyn Contests", desc = "")
    @SearchTag("Agatha")
    @Accordion
    val starlynContest: StarlynContestsConfig = StarlynContestsConfig()

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
    @ConfigOption(name = "Foraging Tracker", desc = "")
    @OnlyModern
    @Accordion
    val tracker = ForagingTrackerConfig()

    @Expose
    @ConfigOption(name = "Mute Phantoms", desc = "Silences Phantoms in the Galatea.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    var mutePhantoms = true

    @Expose
    @ConfigOption(name = "Mute Tree Breaking", desc = "Mutes the sound of the tree fully breaking.")
    @ConfigEditorBoolean
    @FeatureToggle
    @OnlyModern
    var muteTreeBreaking = true

    @Expose
    @ConfigOption(name = "Also on Galatea", desc = "Also mutes tree breaking sounds on Galatea.")
    @ConfigEditorBoolean
    @OnlyModern
    var muteTreeBreakingOnGalatea = true
}
