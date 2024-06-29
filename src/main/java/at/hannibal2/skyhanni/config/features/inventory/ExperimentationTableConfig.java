package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.garden.pests.PestProfitTrackerConfig;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ExperimentationTableConfig {

    @Expose
    @ConfigOption(name = "Experiments Profit Tracker", desc = "")
    @Accordion
    public ExperimentsProfitTrackerConfig experimentsProfitTracker = new ExperimentsProfitTrackerConfig();

    public static class ExperimentsProfitTrackerConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Tracker for drops/XP you get from experiments.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Hide Message", desc = "Hide the messages sent after completing Add-on/Main experiments.")
        @ConfigEditorBoolean
        public boolean hideMessage = false;

        @Expose
        @ConfigOption(name = "Time displayed", desc = "Time displayed after completing an experiment.")
        @ConfigEditorSlider(minValue = 5, maxValue = 60, minStep = 1)
        public int timeDisplayed = 30;

        @Expose
        @ConfigLink(owner = ExperimentsProfitTrackerConfig.class, field = "enabled")
        public Position position = new Position(20, 20, false, true);
    }

    @Expose
    @ConfigOption(name = "Experiments Dry-Streak Display", desc = "")
    @Accordion
    public ExperimentsDryStreakConfig dryStreakConfig = new ExperimentsDryStreakConfig();

    public static class ExperimentsDryStreakConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Display attempts and or XP since your last ULTRA-RARE.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Attempts", desc = "Display Attempts since.")
        @ConfigEditorBoolean
        public boolean attemptsSince = true;

        @Expose
        @ConfigOption(name = "XP", desc = "Display XP since.")
        @ConfigEditorBoolean
        public boolean xpSince = true;

        @Expose
        @ConfigLink(owner = ExperimentsDryStreakConfig.class, field = "enabled")
        public Position dryStreakDisplayPosition = new Position(-220, 70, false, true);
    }

    @Expose
    @ConfigOption(name = "Experiments Display", desc = "Shows a display with useful information while doing experiments.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean experimentationTableDisplay = false;

    @Expose
    @ConfigLink(owner = ExperimentationTableConfig.class, field = "experimentationTableDisplay")
    public Position informationDisplayPosition = new Position(-220, 70, false, true);

    @Expose
    @ConfigOption(name = "Superpairs Clicks Alert", desc = "Display an alert when you reach the maximum clicks gained from Chronomatron or Ultrasequencer.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean superpairsClicksAlert = false;

    @Expose
    @ConfigOption(name = "ULTRA-RARE Book Alert", desc = "Send a chat message, title and sound when you find an ULTRA-RARE book.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean ultraRareBookAlert = false;

    public enum Experiments {
        NONE("", 0),
        BEGINNER("Beginner", 14),
        HIGH("High", 20),
        GRAND("Grand", 20),
        SUPREME("Supreme", 28),
        TRANSCENDENT("Transcendent", 28),
        METAPHYSICAL("Metaphysical", 28),
        ;

        public final String name;
        public final int gridSize;

        Experiments(String name, int gridSize) {
            this.name = name;
            this.gridSize = gridSize;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
