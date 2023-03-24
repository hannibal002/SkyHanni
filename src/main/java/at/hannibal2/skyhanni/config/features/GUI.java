package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorButton;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorKeybind;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;
import org.lwjgl.input.Keyboard;

public class GUI {

    @ConfigOption(
            name = "Edit GUI Locations",
            desc = "Change the position of SkyHanni's overlays"
    )
    @ConfigEditorButton(
            runnableId = "editGuiLocations",
            buttonText = "Edit"
    )
    public Position positions = new Position(-1, -1);

    @Expose
    @ConfigOption(
            name = "Open Hotkey",
            desc = "Press this key to open the GUI Editor."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int keyBindOpen = Keyboard.KEY_NONE;
}
