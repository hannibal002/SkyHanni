package at.hannibal2.skyhanni.config.features.event.diana

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.IndividualItemTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.ItemTrackerGenericConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DianaProfitTrackerConfig : TopLevelTrackerConfig<ItemTrackerGenericConfig> {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Count all items you pick up while doing Diana, " +
            "keeping track of how often you dig burrows, and calculating money earned per burrow."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    override val perTrackerConfig: IndividualItemTrackerConfig = IndividualItemTrackerConfig()

    @Expose
    @ConfigLink(owner = DianaProfitTrackerConfig::class, field = "enabled")
    override val position: Position = Position(20, 20)
}
