package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.garden.GardenIndividualTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WartyCropTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Track §5Warty §7drops from the §dWart Eater Bonus§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat message when receiving a Warty drop.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideChat: Boolean = false

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    val perTrackerConfig: GardenIndividualTrackerConfig = GardenIndividualTrackerConfig()

    @Expose
    @ConfigLink(owner = WartyCropTrackerConfig::class, field = "enabled")
    val position: Position = Position(16, -260)
}
