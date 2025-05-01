package at.hannibal2.skyhanni.config.features.event.diana

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DianaProfitTrackerConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Count all items you pick up while doing Diana, " +
            "keeping track of how often you dig burrows, and calculating money earned per burrow."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = DianaProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(20, 20)
}
