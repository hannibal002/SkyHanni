package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import io.github.moulberry.moulconfig.annotations.*;
import org.lwjgl.input.Keyboard;

public class DevConfig {

    @ConfigOption(name = "Repo Auto Update", desc = "Update the repository on every startup.\n" +
            "§cOnly disable this if you know what you are doing!")
    @ConfigEditorBoolean
    public boolean repoAutoUpdate = true;

    @ConfigOption(name = "Debug", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean debugDO_NOT_USE = false;

    @ConfigOption(name = "Enable Debug", desc = "Enable Test logic")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean debugEnabled = false;

    @ConfigOption(name = "Command Logging", desc = "Logs stack trace information into the console when a command gets sent to hypixel. (by any mod or the player)")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean commandLogs = false;

    @ConfigOption(
            name = "Mod Menu Log",
            desc = "Enables debug messages when the currently opened gui changes, with the path to the gui class. " +
            "Useful for adding more mods to quick mod menu switch."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean modMenuLog = false;

    @ConfigOption(name = "Show internal name", desc = "Show internal names in item lore.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean showInternalName = false;

    @ConfigOption(name = "Show empty internal names", desc = "Shows internal name even if it is blank.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean showEmptyNames = false;

    @ConfigOption(name = "Show item UUID", desc = "Show the Unique Identifier of items. in the lore.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean showItemUuid = false;

    @ConfigOption(name = "Copy Rng Meter", desc = "Copies internal names and maxed xp needed from rng meter inventories in json format into the clipboard.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean copyRngMeter = false;

    @ConfigOption(name = "Highlight Missing Repo Items", desc = "Highlights each item in the current inventory that is not in your current NEU repo.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean highlightMissingRepo = false;

    @ConfigOption(name = "Parkour Waypoints", desc = "")
    @Accordion
    public Waypoints waypoint = new Waypoints();

    public static class Waypoints {

        @ConfigOption(name = "Save Hotkey", desc = "Saves block location to a temporarily parkour and copies everything to your clipboard.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int saveKey = Keyboard.KEY_NONE;

        @ConfigOption(name = "Delete Hotkey", desc = "Deletes the last saved location for when you make a mistake.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int deleteKey = Keyboard.KEY_NONE;

        @ConfigOption(name = "Show Platform Number", desc = "Show the index number over the platform for every parkour.")
        @ConfigEditorBoolean
        public boolean showPlatformNumber = false;

    }

    public Position debugPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Minecraft Console", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean minecraftConsole = false;

    @ConfigOption(name = "Unfiltered Debug", desc = "Print the debug information for unfiltered console messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean printUnfilteredDebugs = false;

    @ConfigOption(name = "Unfiltered Debug File", desc = "Print the debug information into log files instead of into the console for unfiltered console messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean logUnfilteredFile = false;

    @ConfigOption(
            name = "Outside SkyBlock",
            desc = "Print the debug information for unfiltered console messages outside SkyBlock too."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean printUnfilteredDebugsOutsideSkyBlock = false;

    @ConfigOption(
            name = "Log Filtered",
            desc = "Log the filtered messages into the console."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean printFilteredReason = false;

    @ConfigOption(name = "Console Filters", desc = "")
    @ConfigAccordionId(id = 1)
    @ConfigEditorAccordion(id = 2)
    public boolean consoleFilters = false;

    @ConfigOption(name = "Filter Chat", desc = "Filter chat messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterChat = false;

    @ConfigOption(name = "Filter Grow Buffer", desc = "Filter 'Needed to grow BufferBuilder buffer:'")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterGrowBuffer = true;

    @ConfigOption(name = "Filter Sound Error", desc = "Filter 'Unable to play unknown soundEvent'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterUnknownSound = true;

    @ConfigOption(name = "Filter Scoreboard Errors", desc = "Filter error messages with Scoreboard: removeTeam, createTeam, " +
            "removeObjective and 'scoreboard team already exists'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterScoreboardErrors = true;

    @ConfigOption(name = "Filter Particle", desc = "Filter message 'Could not spawn particle effect VILLAGER_HAPPY'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterParticleVillagerHappy = true;

    @ConfigOption(name = "Filter OptiFine", desc = "Filter OptiFine messages CustomItems and ConnectedTextures during loading.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterOptiFine = true;

    @ConfigOption(name = "Filter AsmHelper Transformer", desc = "Filter messages when AsmHelper is Transforming a class during loading.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterAmsHelperTransformer = true;

    @ConfigOption(name = "Filter Applying AsmWriter", desc = "Filter messages when AsmHelper is applying AsmWriter ModifyWriter.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterAsmHelperApplying = true;

    @ConfigOption(name = "Filter Biome ID Bounds", desc = "Filter message 'Biome ID is out of bounds'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterBiomeIdBounds = true;
}
