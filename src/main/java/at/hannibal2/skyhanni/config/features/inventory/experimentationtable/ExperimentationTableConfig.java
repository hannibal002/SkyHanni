package at.hannibal2.skyhanni.config.features.inventory.experimentationtable;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ExperimentationTableConfig {

    @Expose
    @ConfigOption(name = "Profit Tracker", desc = "")
    @Accordion
    public ExperimentsProfitTrackerConfig experimentsProfitTracker = new ExperimentsProfitTrackerConfig();

    @Expose
    @ConfigOption(name = "Dry-Streak Display", desc = "")
    @Accordion
    public ExperimentsDryStreakConfig dryStreak = new ExperimentsDryStreakConfig();

    @Expose
    @ConfigOption(name = "Experiment Addons", desc = "")
    @Accordion
    public ExperimentsAddonsConfig addons = new ExperimentsAddonsConfig();

    @Expose
    @ConfigOption(name = "Superpairs", desc = "")
    @Accordion
    public ExperimentsSuperpairsConfig superpairs = new ExperimentsSuperpairsConfig();

    @Expose
    @ConfigOption(
        name = "Guardian Reminder",
        desc = "Sends a warning when opening the Experimentation Table without a §9§lGuardian Pet §7equipped."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean guardianReminder = false;
}
