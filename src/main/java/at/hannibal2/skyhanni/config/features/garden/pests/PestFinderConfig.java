package at.hannibal2.skyhanni.config.features.garden.pests;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class PestFinderConfig {

    @Expose
    @ConfigOption(
            name = "Display",
            desc = "Show a display with all know pest locations."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showDisplay = true;

    @Expose
    @ConfigOption(
            name = "Show Plot in World",
            desc = "Mark infected plot names and world border in the world."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showPlotInWorld = true;

    @Expose
    @ConfigOption(
            name = "Only With Vacuum",
            desc = "Only show the pest display and waypoints while holding a vacuum in the hand."
    )
    @ConfigEditorBoolean
    public boolean onlyWithVacuum = true;

    @Expose
    public Position position = new Position(-350, 200, 1.3f);

    @Expose
    @ConfigOption(name = "Teleport Hotkey", desc = "Press this key to warp to the nearest plot with pests on it.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int teleportHotkey = Keyboard.KEY_NONE;
}
