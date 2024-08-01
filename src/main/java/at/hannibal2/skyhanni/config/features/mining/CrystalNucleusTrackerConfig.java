package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class CrystalNucleusTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the Crystal Nucleus Tracker overlay for mining.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigLink(owner = CrystalNucleusTrackerConfig.class, field = "enabled")
    public Position position = new Position(20, 20, false, true);

    @Expose
    @ConfigOption(name = "Show Outside of Nucleus", desc = "Show the tracker anywhere in the Crystal Hollows.")
    @ConfigEditorBoolean
    public boolean showEverywhere = false;

    @Expose
    @ConfigOption(name = "Profit Per", desc = "Show profit summary message for the completed nucleus run.")
    @ConfigEditorBoolean
    public boolean profitPer = true;

    @Expose
    @ConfigOption(name = "Chat Modification Type", desc = "What should be done about the long run summary messages.")
    @ConfigEditorDropdown
    public CrystalNucleusTrackerFilterEntry chatModificationType = CrystalNucleusTrackerFilterEntry.SHOW_ABOVE;

    public enum CrystalNucleusTrackerFilterEntry {
        HIDE("Hide Completely"),
        SHOW("Show Completely"),
        SHOW_ABOVE("Show Above §6_ coins"),
        COMPACT("Compact");

        private final String str;

        CrystalNucleusTrackerFilterEntry(String str) { this.str = str; }

        @Override
        public String toString() { return str; }
    }

    @Expose
    @ConfigOption(name = "Coin Threshold", desc = "How many coins loot must be worth to be shown with §7\"§rShow Above §6_ coins§7\" enabled.")
    @ConfigEditorSlider(minValue = 0, maxValue = 1000000000, minStep = 25000)
    public int chatModificationThresholdValue = 10000;
}
