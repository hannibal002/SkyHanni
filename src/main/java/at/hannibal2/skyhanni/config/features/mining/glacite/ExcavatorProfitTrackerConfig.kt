package at.hannibal2.skyhanni.config.features.mining.glacite

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.IndividualItemTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

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
    @ConfigOption(name = "Ironman Profits", desc = "Select which profiles should use the Ironman price calculation option. §eRemoves the cost of Scrap from the Profit.")
    @ConfigEditorDropdown
    val ironmanProfitType: Property<IronmanProfitType> = Property.of(IronmanProfitType.ONLY_IRONMAN)

    enum class IronmanProfitType(private val displayName: String) {
        NONE("§cNone"),
        ONLY_IRONMAN("§7Only Ironman"),
        ALL_PROFILES("§2All Profiles");
        override fun toString(): String = displayName
    }

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    val perTrackerConfig: IndividualItemTrackerConfig = IndividualItemTrackerConfig()

    @Expose
    @ConfigLink(owner = ExcavatorProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(-380, 150)
}
