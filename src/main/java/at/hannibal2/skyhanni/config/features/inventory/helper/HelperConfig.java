package at.hannibal2.skyhanni.config.features.inventory.helper;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class HelperConfig {
    @Expose
    @ConfigOption(name = "Melody's Hair Harp", desc = "")
    @Accordion
    public HarpConfig harp = new HarpConfig();

    public static class HarpConfig {
        @Expose
        @ConfigOption(name = "GUI Scale", desc = "Automatically sets the GUI scale to \"AUTO\" when entering the Harp")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean guiScale = false;
        @Expose
        @ConfigOption(name = "Quick Restart", desc = "When pressing the close Button in the Harp Menu shortly after opening the Harp the selected song will be started")
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
        @ConfigOption(name = "Keybinds", desc = "")
        @Accordion
        public HarpConfigKeyBinds harpKeybinds = new HarpConfigKeyBinds();
    }

    @Expose
    @ConfigOption(name = "Tia Relay Abiphone Network Maintenance", desc = "")
    @Accordion
    public TiaRelayConfig tiaRelay = new TiaRelayConfig();
}
