package at.hannibal2.skyhanni.config.features.crimsonisle;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class ReputationHelperConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable features around Reputation features in the Crimson Isle.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Use Hotkey", desc = "Only show the Reputation Helper while pressing the hotkey.")
    @ConfigEditorBoolean
    public boolean useHotkey = false;

    @Expose
    @ConfigOption(name = "Hotkey", desc = "Press this hotkey to show the Reputation Helper.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int hotkey = Keyboard.KEY_NONE;


    @Expose
    public Position position = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Show Locations", desc = "Crimson Isles waypoints for locations to get reputation.")
    @ConfigEditorDropdown(values = {"Always", "Only With Hotkey", "Never"})
    public int showLocation = 1;
}
