package at.hannibal2.skyhanni.config.features.slayer

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SlayerTimeMessagesConfig {
    @Expose
    @ConfigOption(name = "Time to Kill", desc = "Sends time to kill a slayer in chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    var timeToKill: Boolean = true

    @Expose
    @ConfigOption(name = "Time to Kill Personal Bests", desc = "Sends personal best for killing a slayer in chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    var timeToKillPersonalBests: Boolean = false

    @Expose
    @ConfigOption(name = "Quest Complete", desc = "Sends time to complete (Spawn & Kill) a slayer quest in chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    var questComplete: Boolean = true

    @Expose
    @ConfigOption(name = "Compact Time Messages", desc = "Uses shorter chat messages.")
    @ConfigEditorBoolean
    var compact: Boolean = false

    @Expose
    @ConfigOption(name = "Show Titles", desc = "Displays slayer messages as titles.")
    @ConfigEditorBoolean
    @FeatureToggle
    var titles: Boolean = false

    @Expose
    @Accordion
    @ConfigOption(name = "Templates", desc = "Customize the displayed messages.")
    val templates = SlayerTimeMessagesTemplatesConfig()
}
