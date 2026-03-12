package at.hannibal2.skyhanni.config.features.slayer

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
import io.github.notenoughupdates.moulconfig.observer.Property

class SlayerProfitTrackerConfig : TopLevelTrackerConfig<ItemTrackerGenericConfig> {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Count all items you pick up while doing slayer, " +
            "keeping track of how much you pay for starting slayers and calculating the overall profit.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = SlayerProfitTrackerConfig::class, field = "enabled")
    override val position: Position = Position(20, 20)

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    override val perTrackerConfig: IndividualItemTrackerConfig = IndividualItemTrackerConfig()
}

