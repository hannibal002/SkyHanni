package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.ItemTrackerSettings
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.PerTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ExcavatorProfitTrackerConfig : TopLevelTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Count all drops you gain while excavating in the Fossil Research Center.")
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Track Glacite Powder",
        desc = "Track Glacite Powder gained as well (no profit, but progress)."
    )
    @ConfigEditorBoolean
    var trackGlacitePowder: Boolean = true

    @Expose
    @ConfigOption(name = "Track Fossil Dust", desc = "Track Fossil Dust and use it for profit calculation.")
    @ConfigEditorBoolean
    var showFossilDust: Boolean = true

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    override val perTrackerConfig: PerTrackerConfig<ItemTrackerSettings> = PerTrackerConfig()

    @Expose
    @ConfigLink(owner = ExcavatorProfitTrackerConfig::class, field = "enabled")
    override val position: Position = Position(-380, 150)
}
