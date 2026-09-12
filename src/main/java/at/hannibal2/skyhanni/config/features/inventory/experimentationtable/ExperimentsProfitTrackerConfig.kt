package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.IndividualItemTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ExperimentsProfitTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Tracker for drops/XP you get from experiments.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Messages", desc = "Change the messages to be hidden after completing Add-on/Main experiments.")
    @ConfigEditorDraggableList
    val hideMessages: MutableList<ExperimentationTableApi.ExperimentationMessages> = mutableListOf()

    @Expose
    @ConfigOption(name = "Ironman Profits", desc = "Select which profiles should use the Ironman price calculation option. §eRemoves the cost of Experience Bottles from the Profit.")
    @ConfigEditorDropdown
    val ironmanProfitType: Property<IronmanProfitType> = Property.of(IronmanProfitType.ONLY_IRONMAN)

    enum class IronmanProfitType(private val displayName: String) {
        NONE("§cNone"),
        ONLY_IRONMAN("§7Only Ironman"),
        ALL_PROFILES("§2All Profiles");
        override fun toString(): String = displayName
    }

    @Expose
    @ConfigOption(name = "Track Time Spent", desc = "Track time spent doing addons and experiments.")
    @ConfigEditorBoolean
    var trackTimeSpent: Boolean = false

    @Expose
    @ConfigOption(name = "Track Used Bottles", desc = "Track thrown XP bottles while near the experimentation table.")
    @ConfigEditorBoolean
    var trackUsedBottles: Boolean = true

    @Expose
    @ConfigOption(name = "Bottle Warnings", desc = "Display warnings once per session about bottles being auto-tracked.")
    @ConfigEditorBoolean
    var bottleWarnings: Boolean = true

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = "",
    )
    @Accordion
    val perTrackerConfig: IndividualItemTrackerConfig = IndividualItemTrackerConfig()

    @Expose
    @ConfigLink(owner = ExperimentsProfitTrackerConfig::class, field = "enabled")
    val position: Position = Position(20, 20)
}
