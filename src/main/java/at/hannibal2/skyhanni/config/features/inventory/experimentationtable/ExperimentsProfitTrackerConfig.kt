package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.ItemTrackerSettings
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.PerTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ExperimentsProfitTrackerConfig : TopLevelTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Tracker for drops/XP you get from experiments.")
    @ConfigEditorBoolean
    @FeatureToggle
    override var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Hide Messages", desc = "Change the messages to be hidden after completing Add-on/Main experiments.")
    @ConfigEditorDraggableList
    val hideMessages: MutableList<ExperimentationTableApi.ExperimentationMessages> = mutableListOf()

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
    @ConfigOption(name = "Tracker Settings", desc = "")
    @Accordion
    override val perTrackerConfig: PerTrackerConfig<ItemTrackerSettings> = PerTrackerConfig()

    @Expose
    @ConfigLink(owner = ExperimentsProfitTrackerConfig::class, field = "enabled")
    override val position: Position = Position(20, 20)
}
