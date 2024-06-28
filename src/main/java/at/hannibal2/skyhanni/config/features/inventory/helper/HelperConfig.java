package at.hannibal2.skyhanni.config.features.inventory.helper;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HelperConfig {
    @Expose
    @ConfigOption(name = "Melody's Hair Harp", desc = "")
    @Accordion
    public HarpConfig harp = new HarpConfig();

    public static class HarpConfig {
        @Expose
        @ConfigOption(name = "GUI Scale", desc = "Automatically set the GUI scale to \"AUTO\" when entering the Harp.")
        @ConfigEditorBoolean
        public boolean guiScale = false;

        @Expose
        @ConfigOption(name = "Quick Restart", desc = "Once you've launched the Harp, quickly hit the close button in the Harp Menu to initiate the selected song.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean quickRestart = false;

        @Expose
        @ConfigOption(name = "Use Keybinds", desc = "In the Harp, press buttons with your number row on the keyboard instead of clicking.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean keybinds = false;

        @Expose
        @ConfigOption(name = "Show Numbers", desc = "In the Harp, show buttons as stack size (intended to be used with the Keybinds).")
        @ConfigEditorBoolean
        public boolean showNumbers = false;

        @Expose
        @ConfigOption(name = "Hide Tooltip", desc = "Hide the item tooltips inside the Harp.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideMelodyTooltip = false;

        @Expose
        @ConfigOption(name = "Keybinds", desc = "")
        @Accordion
        public HarpConfigKeyBinds harpKeybinds = new HarpConfigKeyBinds();
    }

    @Expose
    @ConfigOption(name = "Tia Relay Abiphone Network Maintenance", desc = "")
    @Accordion
    public TiaRelayConfig tiaRelay = new TiaRelayConfig();

    @Expose
    @ConfigOption(name = "Reforge Helper", desc = "")
    @Accordion
    public ReforgeHelperConfig reforge = new ReforgeHelperConfig();

    @Expose
    @ConfigOption(name = "Enchanting", desc = "")
    @Accordion
    public EnchantingConfig enchanting = new EnchantingConfig();

    public static class EnchantingConfig {
        @Expose
        @ConfigOption(name = "Experimentation Table Display", desc = "Shows a display with useful information for the experimentation table while doing experiments.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean experimentationTableDisplay = false;

        @Expose
        @ConfigLink(owner = EnchantingConfig.class, field = "experimentationTableDisplay")
        public Position informationDisplayPosition = new Position(0, 200, true, true);

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
    }

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
