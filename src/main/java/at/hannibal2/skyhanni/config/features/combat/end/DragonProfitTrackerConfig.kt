package at.hannibal2.skyhanni.config.features.combat.end

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

class DragonProfitTrackerConfig : TopLevelTrackerConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Keeps track of everything you pick up while fighting the dragon, " +
            "keeping track of how much you pay for starting the fight and calculating the overall profit."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = true

    @Expose
    @ConfigLink(owner = DragonProfitTrackerConfig::class, field = "enabled")
    override val position: Position = Position(20, 20)

    @Expose
    @ConfigOption(
        name = "Count Leeched Dragons",
        desc = "Count Dragons you placed no eyes in towards your total profit."
    )
    @FeatureToggle
    @ConfigEditorBoolean
    var countLeechedDragons: Boolean = true

    @Expose
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    override val perTrackerConfig: PerTrackerConfig<ItemTrackerSettings> = PerTrackerConfig()
}
