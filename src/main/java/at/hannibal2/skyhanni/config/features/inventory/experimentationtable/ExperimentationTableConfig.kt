package at.hannibal2.skyhanni.config.features.inventory.experimentationtable

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ExperimentationTableConfig {
    @Expose
    @ConfigOption(name = "Profit Tracker", desc = "")
    @Accordion
    val experimentsProfitTracker: ExperimentsProfitTrackerConfig = ExperimentsProfitTrackerConfig()

    @Expose
    @ConfigOption(name = "Dry-Streak Display", desc = "")
    @Accordion
    val dryStreak: ExperimentsDryStreakConfig = ExperimentsDryStreakConfig()

    @Expose
    @ConfigOption(name = "Experiment Addons", desc = "")
    @Accordion
    val addons: ExperimentsAddonsConfig = ExperimentsAddonsConfig()

    @Expose
    @ConfigOption(name = "Superpairs", desc = "")
    @Accordion
    val superpairs: ExperimentsSuperpairsConfig = ExperimentsSuperpairsConfig()

    @Expose
    @ConfigOption(
        name = "Guardian Reminder",
        desc = "Sends a warning when opening the Experimentation Table without a §9§lGuardian Pet §7equipped."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var guardianReminder: Boolean = false
}
