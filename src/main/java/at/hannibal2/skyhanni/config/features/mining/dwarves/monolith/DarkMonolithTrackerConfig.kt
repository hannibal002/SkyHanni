package at.hannibal2.skyhanni.config.features.mining.dwarves.monolith

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

class DarkMonolithTrackerConfig : TopLevelTrackerConfig<ItemTrackerGenericConfig> {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Track mithril powder, coins, and Rock the Fish drops obtained from collecting Dark Monoliths.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = DarkMonolithTrackerConfig::class, field = "enabled")
    override val position: Position = Position(100, 100)

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = "",
    )
    @Accordion
    override val perTrackerConfig: IndividualItemTrackerConfig = IndividualItemTrackerConfig()
}
