package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.data.GuiEditManager;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

public class GUIConfig {

    @ConfigOption(name = "Edit GUI Locations", desc = "Change the position of SkyHanni's overlays")
    @ConfigEditorButton(buttonText = "Edit")
    public Runnable positions = GuiEditManager::openGuiPositionEditor;

    @Expose
    @ConfigOption(name = "Open Hotkey", desc = "Press this key to open the GUI Editor.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int keyBindOpen = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Global GUI scale", desc = "Globally scale all SkyHanni GUIs")
    @ConfigEditorSlider(minValue = 0.1F, maxValue = 10, minStep = 0.05F)
    public float globalScale = 1F;

    @Expose
    @ConfigOption(name = "Custom Text box", desc = "")
    @Accordion
    public TextBoxConfig customTextBox = new TextBoxConfig();

    public static class TextBoxConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Enables showing the textbox while in SkyBlock.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Text", desc = "Enter text you want to display here.\n" +
                "§eUse '&' as the colour code character.\n" +
                "§eUse '\\n' as the line break character.")
        @ConfigEditorText
        public Property<String> text = Property.of("&aYour Text Here\\n&bYour new line here");

        @Expose
        public Position position = new Position(10, 80, false, true);
    }

    @Expose
    @ConfigOption(name = "Real Time", desc = "Display the current computer time, a handy feature when playing in full-screen mode.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean realTime = false;

    @Expose
    @ConfigOption(name = "Real Time 12h Format", desc = "Display the current computer time in 12hr Format rather than 24h Format.")
    @ConfigEditorBoolean
    public boolean realTimeFormatToggle = false;

    @Expose
    public Position realTimePosition = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Tps Display", desc = "Show the TPS of the current server, like in Soopy.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean tpsDisplay = false;

    @Expose
    public Position tpsDisplayPosition = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Config Button", desc = "Add a button to the pause menu to configure SkyHanni.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean configButtonOnPause = true;
}
