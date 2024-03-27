package at.hannibal2.skyhanni.config.features.dev;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.dev.minecraftconsole.MinecraftConsoleConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.Category;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class DevConfig {

    @Expose
    @ConfigOption(name = "Repo Auto Update", desc = "Update the repository on every startup.\n" +
        "§cOnly disable this if you know what you are doing!")
    @ConfigEditorBoolean
    public boolean repoAutoUpdate = true;

    @Expose
    @ConfigOption(name = "Log Expiry Time", desc = "Deletes your SkyHanni logs after this time period in days.")
    @ConfigEditorSlider(minValue = 1, maxValue = 30, minStep = 1)
    public int logExpiryTime = 14;

    @Expose
    @ConfigOption(name = "Debug", desc = "")
    @Accordion
    public DebugConfig debug = new DebugConfig();

    @Expose
    @ConfigOption(name = "Repo Pattern", desc = "")
    @Accordion
    public RepoPatternConfig repoPattern = new RepoPatternConfig();

    @Expose
    @ConfigOption(name = "Slot Number", desc = "Show slot number in inventory while pressing this key.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int showSlotNumberKey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "World Edit", desc = "Use wood axe or command /shworldedit to render a box, similar like the WorldEdit plugin.")
    @ConfigEditorBoolean
    public boolean worldEdit = false;

    @Expose
    @ConfigOption(name = "Bow Sound distance", desc = "The distance in blocks where the sound of shooting a bow will be used for the QuiverAPI.")
    @ConfigEditorSlider(minValue = 0, maxValue = 50, minStep = 1)
    public int bowSoundDistance = 5;

    @Expose
    @ConfigOption(name = "Unknown Lines warning", desc = "Gives a chat warning when unknown lines are found in the scoreboard." +
        "\nCustom Scoreboard debug option")
    @ConfigEditorBoolean
    public boolean unknownLinesWarning = false;

    @ConfigOption(name = "Parkour Waypoints", desc = "")
    @Accordion
    @Expose
    public WaypointsConfig waypoint = new WaypointsConfig();

    @Expose
    public Position debugPos = new Position(10, 10, false, true);

    @Expose
    public Position debugLocationPos = new Position(1, 160, false, true);

    @Expose
    @Category(name = "Minecraft Console", desc = "Minecraft Console Settings")
    public MinecraftConsoleConfig minecraftConsoles = new MinecraftConsoleConfig();

}
