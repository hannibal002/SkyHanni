package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class ExperimentationTableConfig {
    @Expose
    @ConfigOption(name = "Profit Tracker", desc = "")
    @Accordion
    val experimentsProfitTracker: ExperimentsProfitTrackerConfig = ExperimentsProfitTrackerConfig()

    @Expose
    @ConfigOption(name = "Dry-Streak Display", desc = "")
    @Accordion
    @SearchTag("enchant enchanting")
    val dryStreak: ExperimentsDryStreakConfig = ExperimentsDryStreakConfig()

    @Expose
    @ConfigOption(name = "Experiment Addons", desc = "")
    @Accordion
    @SearchTag("enchant enchanting")
    val addons: ExperimentsAddonsConfig = ExperimentsAddonsConfig()

    @Expose
    @ConfigOption(name = "Superpairs", desc = "")
    @Accordion
    @SearchTag("enchant enchanting")
    val superpairs: ExperimentsSuperpairsConfig = ExperimentsSuperpairsConfig()

    @Expose
    @ConfigOption(
        name = "Guardian Reminder",
        desc = "Sends a warning when opening the Experimentation Table without a §9§lGuardian Pet §7equipped."
    )
    @ConfigEditorBoolean
    @SearchTag("enchant enchanting")
    @FeatureToggle
    var guardianReminder: Boolean = false
}
