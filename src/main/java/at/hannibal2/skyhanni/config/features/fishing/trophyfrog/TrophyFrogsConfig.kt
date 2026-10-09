package at.hannibal2.skyhanni.config.features.fishing.trophyfrog

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TrophyFrogsConfig {
    @Expose
    @ConfigOption(name = "Trophy Frog Chat Messages", desc = "")
    @Accordion
    val chatMessages: ChatMessagesConfig = ChatMessagesConfig()

    @Expose
    @ConfigOption(name = "Trophy Frog Display", desc = "")
    @Accordion
    val display: TrophyFrogDisplayConfig = TrophyFrogDisplayConfig()

    @Expose
    @ConfigOption(name = "Total Caught Tooltip", desc = "Show total Trophy Frogs caught in the Researcher Ribery menu tooltip.")
    @ConfigEditorBoolean
    @FeatureToggle
    var totalFrogsCaught: Boolean = true
}
