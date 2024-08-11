package at.hannibal2.skyhanni.config.features.garden.customkeybinds;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

public class KeyBindCrop {

//     TODO: find out why the game crashes on button press, even with no code in the runnable

    @ConfigOption(name = "test button", desc = "test button")
    @ConfigEditorButton(buttonText = "test")
    public Runnable testButton = () -> {
    };

    @ConfigOption(name = "Disable All", desc = "Disable all keys of this crop.")
    @ConfigEditorButton(buttonText = "Disable")
    public Runnable presetDisableCropLevel = () -> {
        attack = Keyboard.KEY_NONE;
        useItem = Keyboard.KEY_NONE;
        left = Keyboard.KEY_NONE;
        right = Keyboard.KEY_NONE;
        forward = Keyboard.KEY_NONE;
        back = Keyboard.KEY_NONE;
        jump = Keyboard.KEY_NONE;
        sneak = Keyboard.KEY_NONE;
    };

    @ConfigOption(name = "Set Default", desc = "Reset all keys of this crop to default.")
    @ConfigEditorButton(buttonText = "Default")
    public Runnable presetDefaultCropLevel = () -> {
        attack = -100;
        useItem = -99;
        left = Keyboard.KEY_A;
        right = Keyboard.KEY_D;
        forward = Keyboard.KEY_W;
        back = Keyboard.KEY_S;
        jump = Keyboard.KEY_SPACE;
        sneak = Keyboard.KEY_LSHIFT;
    };


    @Expose
    @ConfigOption(name = "Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = -100)
    public int attack = -100;

    @Expose
    @ConfigOption(name = "Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = -99)
    public int useItem = -99;

    @Expose
    @ConfigOption(name = "Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int left = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int right = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int forward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int back = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int jump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int sneak = Keyboard.KEY_LSHIFT;

}
