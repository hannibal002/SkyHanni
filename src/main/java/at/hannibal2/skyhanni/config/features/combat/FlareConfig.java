package at.hannibal2.skyhanni.config.features.combat;

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
    @ConfigOption(name = "Enable", desc = "Show current active flares.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Alert Type", desc = "What type of alert should be sent when a flare is about to expire.")
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
    @ConfigOption(name = "Display Type", desc = "Where to show the timer, as GUI element or in the world")
    @ConfigEditorDropdown
    public DisplayType displayType = DisplayType.GUI;

    public enum DisplayType {
        GUI("GUI Element"),
        WORLD("In World"),
        BOTH("Both"),
        ;

        private final String str;

        DisplayType(String str) {
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
    public OutlineType outlineType = OutlineType.NONE;

    public enum OutlineType {
        NONE("No Outline"),
        FILLED("Filled"),
        WIREFRAME("Wireframe"),
        CIRCLE("Circle")
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
    @ConfigOption(name = "Warning Flare Color", desc = "Color for Warning Flare.")
    @ConfigEditorColour
    public String warningColor = "0:153:29:255:136";

    @Expose
    @ConfigOption(name = "Alert Flare Color", desc = "Color for Alert Flare.")
    @ConfigEditorColour
    public String alertColor = "0:153:0:159:137";

    @Expose
    @ConfigOption(name = "SOS Flare Color", desc = "Color for SOS Flare.")
    @ConfigEditorColour
    public String sosColor = "0:153:159:0:5";

    @Expose
    @ConfigLink(owner = FlareConfig.class, field = "enabled")
    public Position position = new Position(150, 200, false, true);

    @Expose
    @ConfigOption(name = "Show Buff", desc = "Show the mana regen buff next to the flare name.")
    @ConfigEditorBoolean
    public boolean showManaBuff = false;

    @Expose
    @ConfigOption(name = "Hide particles", desc = "Hide flame particles spawning around the flare.")
    @ConfigEditorBoolean
    public boolean hideParticles = false;
}
