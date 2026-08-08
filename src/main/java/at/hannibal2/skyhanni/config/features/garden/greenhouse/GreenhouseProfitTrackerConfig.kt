package at.hannibal2.skyhanni.config.features.garden.greenhouse

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.garden.GardenIndividualItemTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GreenhouseProfitTrackerConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Track the profit from items gained while on Greenhouse plots.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    val perTrackerConfig: GardenIndividualItemTrackerConfig = GardenIndividualItemTrackerConfig()

    @Expose
    @ConfigLink(owner = GreenhouseProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(180, 60)
}
