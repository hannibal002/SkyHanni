package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FlareConfig {

    @Expose
    @ConfigOption(name = "Enable Overlay", desc = "Show current active flares.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean overlayEnabled = false;

    @Expose
    @ConfigOption(name = "Alert", desc = "Send an alert when a flare is about to expire.")
    @ConfigEditorDropdown
    public AlertType alertType = AlertType.CHAT;

    public enum AlertType {
        NONE("No alert"),
        CHAT("Chat"),
        TITLE("Title"),
        CHAT_TITLE("Chat & Title"),
        ;

        private final String str;

        AlertType(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Show Effective Area", desc = "Show the effective area of the Flare.")
    @ConfigEditorDropdown
    public OutlineType outlineType = OutlineType.FILLED;

    public enum OutlineType {
        NONE("No Outline"),
        FILLED("Filled"),
        WIREFRAME("Wireframe"),
        ;

        private final String str;

        OutlineType(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Color of the area", desc = "The color of the area.")
    @ConfigEditorColour
    public String color = "0:153:18:159:85";

    @Expose
    @ConfigLink(owner = FlareConfig.class, field = "overlayEnabled")
    public Position position = new Position(150, 200, false, true);
}
