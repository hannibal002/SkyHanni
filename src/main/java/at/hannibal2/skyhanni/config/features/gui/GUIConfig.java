package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.data.GuiEditManager;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class GUIConfig {

    @ConfigOption(name = "Edit GUI Locations", desc = "Change the position of SkyHanni's overlays.")
    @ConfigEditorButton(buttonText = "Edit")
    public Runnable positions = GuiEditManager::openGuiPositionEditor;

    @Expose
    @ConfigOption(name = "Open Hotkey", desc = "Press this key to open the GUI Editor.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int keyBindOpen = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Global GUI Scale", desc = "Globally scale all SkyHanni GUIs.")
    @ConfigEditorSlider(minValue = 0.1F, maxValue = 10, minStep = 0.05F)
    public float globalScale = 1F;

    @Expose
    @ConfigOption(name = "Modify Visual Words", desc = "")
    @Accordion
    public ModifyWordsConfig modifyWords = new ModifyWordsConfig();

    @Expose
    @ConfigOption(name = "Custom Text Box", desc = "")
    @Accordion
    public TextBoxConfig customTextBox = new TextBoxConfig();

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
    @ConfigOption(name = "In-Game Date", desc = "")
    @Accordion
    public InGameDateConfig inGameDate = new InGameDateConfig();

    @Expose
    @ConfigOption(name = "TPS Display", desc = "Show the TPS of the current server, like in Soopy.")
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
