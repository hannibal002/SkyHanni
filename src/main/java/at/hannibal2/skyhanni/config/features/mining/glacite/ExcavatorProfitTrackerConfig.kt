package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ExcavatorProfitTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Count all drops you gain while excavating in the Fossil Research Center.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

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
    @ConfigLink(owner = ExcavatorProfitTrackerConfig::class, field = "enabled")
    var position: Position = Position(-380, 150)
}
