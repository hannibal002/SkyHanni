package at.hannibal2.skyhanni.config.features.misc;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class HideArmorConfig {

    @Expose
    @ConfigOption(name = "Mode", desc = "Hide the armor of players.")
    @ConfigEditorDropdown
    public ModeEntry mode = ModeEntry.OFF;

    public enum ModeEntry {
        ALL("All"),
        OWN("Own Armor"),
        OTHERS("Other's Armor"),
        OFF("Off");
        private final String displayName;

        ModeEntry(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Expose
    @ConfigOption(name = "Only Helmet", desc = "Only hide the helmet.")
    @ConfigEditorBoolean
    public Boolean onlyHelmet = false;

}
