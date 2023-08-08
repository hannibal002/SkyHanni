package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.data.GuiEditManager;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class GUIConfig {

    @ConfigOption(name = "Edit GUI Locations", desc = "Change the position of SkyHanni's overlays")
    @ConfigEditorButton(buttonText = "Edit")
    public Runnable positions = GuiEditManager::openGuiPositionEditor;

    @Expose
    @ConfigOption(name = "Open Hotkey", desc = "Press this key to open the GUI Editor.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int keyBindOpen = Keyboard.KEY_NONE;
}
