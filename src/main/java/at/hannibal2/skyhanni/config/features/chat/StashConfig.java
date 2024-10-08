package at.hannibal2.skyhanni.config.features.chat;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class StashConfig {

    @Expose
    @ConfigOption(name = "Stash Warnings", desc = "Compact or hide warnings relating to items/materials in your stash.")
    @ConfigEditorDropdown
    public StashHandlerType stashWarnings = StashHandlerType.COMPACT;

    public enum StashHandlerType {
        NONE("None (Use Hypixel messages)"),
        COMPACT("Compact Messages"),
        HIDE("Hide Completely"),
        ;

        private final String name;

        StashHandlerType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Expose
    @ConfigOption(name = "Use /ViewStash", desc = "Use /viewstash [type] instead of /pickupstash.")
    @ConfigEditorBoolean
    public boolean useViewStash = false;
}
