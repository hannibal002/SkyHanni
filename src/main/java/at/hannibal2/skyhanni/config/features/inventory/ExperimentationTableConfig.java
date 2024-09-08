package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.List;

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
        @ConfigOption(name = "Hide Messages", desc = "Change the messages to be hidden after completing Add-on/Main experiments.")
        @ConfigEditorDraggableList
        public List<ExperimentMessages> hideMessages = new ArrayList<>();

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

    @Expose
    @ConfigOption(name = "Guardian Reminder", desc = "Sends a warning when opening the Experimentation Table without a §9§lGuardian Pet §7equipped.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean guardianReminder = false;

    public enum ExperimentMessages {
        DONE("§eYou claimed the §dSuperpairs §erewards!"),
        EXPERIENCE("§8 +§3141k Experience §8(§7Experience Drops§8)"),
        ENCHANTMENTS("§8 +§9Smite VII §8(§7Enchantment Drops§8)"),
        BOTTLES("§8 +§9Titanic Experience Bottle §8(§7Bottle Drops§8)"),
        SERUM("§8 +§5Metaphysical Serum"),
        FISH("§8 +§cExperiment The Fish")
        ;

        private final String str;

        ExperimentMessages(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    public enum Experiments {
        NONE("", 0, 0, 0, 0),
        BEGINNER("Beginner", 14, 18, 35, 1),
        HIGH("High", 20, 10, 43, 2),
        GRAND("Grand", 20, 10, 43, 2),
        SUPREME("Supreme", 28, 9, 44, 1),
        TRANSCENDENT("Transcendent", 28, 9, 44, 1),
        METAPHYSICAL("Metaphysical", 28, 9, 44, 1),
        ;

        public final String name;
        public final int gridSize;
        public final int startSlot;
        public final int endSlot;
        public final int sideSpace;

        Experiments(String name, int gridSize, int startSlot, int endSlot, int sideSpace) {
            this.name = name;
            this.gridSize = gridSize;
            this.startSlot = startSlot;
            this.endSlot = endSlot;
            this.sideSpace = sideSpace;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
