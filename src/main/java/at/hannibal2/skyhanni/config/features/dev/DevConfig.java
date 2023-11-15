package at.hannibal2.skyhanni.config.features.dev;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.dev.minecraftconsole.MinecraftConsoleConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class DevConfig {

    @Expose
    @ConfigOption(name = "Repo Auto Update", desc = "Update the repository on every startup.\n" +
        "§cOnly disable this if you know what you are doing!")
    @ConfigEditorBoolean
    public boolean repoAutoUpdate = true;

    @Expose
    @ConfigOption(name = "Debug", desc = "")
    @Accordion
    public DebugConfig debug = new DebugConfig();

    @Expose
    @ConfigOption(name = "Slot Number", desc = "Show slot number in inventory while pressing this key.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int showSlotNumberKey = Keyboard.KEY_NONE;

    @ConfigOption(name = "Parkour Waypoints", desc = "")
    @Accordion
    @Expose
    public WaypointsConfig waypoint = new WaypointsConfig();

    @Expose
    public Position debugPos = new Position(10, 10, false, true);

    @Expose
    public Position debugLocationPos = new Position(1, 160, false, true);

    @Expose
    @ConfigOption(name = "Minecraft Console", desc = "")
    @Accordion
    public MinecraftConsoleConfig minecraftConsoles = new MinecraftConsoleConfig();

}
