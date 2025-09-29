package at.hannibal2.skyhanni.config.features.garden.composter

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.garden.pests.PestProfitTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.timed.TimedGardenIndividualItemTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ComposterProfitTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Track crops spent and compost earned.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Only in Composter", desc = "Only show tracker when in the composter menu.")
    @ConfigEditorBoolean
    var onlyInInventory: Boolean = true

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    val perTrackerConfig: TimedGardenIndividualItemTrackerConfig = TimedGardenIndividualItemTrackerConfig()

    @Expose
    @ConfigLink(owner = PestProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(20, 20)
}
