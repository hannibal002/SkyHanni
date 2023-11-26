package at.hannibal2.skyhanni.config.features.garden.pests;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class PestSpawnConfig {

    @Expose
    @ConfigOption(
        name = "Chat Message Format",
        desc = "Change how the pest spawn chat message should be formatted.")
    @ConfigEditorDropdown(values = {"Hypixel Style", "Compact", "Disabled"})
    public int chatMessageFormat = 0;

    @Expose
    @ConfigOption(
        name = "Show Title",
        desc = "Show a Title when a pest spawns."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showTitle = true;

    @Expose
    @ConfigOption(name = "Teleport Hotkey", desc = "Press this key to warp to the plot where the last pest has spawned.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int teleportHotkey = Keyboard.KEY_NONE;
}
