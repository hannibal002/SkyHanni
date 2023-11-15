package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
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

    public static class DebugConfig {
        @Expose
        @ConfigOption(name = "Enable Debug", desc = "Enable Test logic")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Command Logging", desc = "Logs stack trace information into the console when a command gets sent to Hypixel. (by any mod or the player)")
        @ConfigEditorBoolean
        public boolean commandLogs = false;

        @Expose
        @ConfigOption(
            name = "Mod Menu Log",
            desc = "Enables debug messages when the currently opened GUI changes, with the path to the gui class. " +
                "Useful for adding more mods to quick mod menu switch."
        )
        @ConfigEditorBoolean
        public boolean modMenuLog = false;

        @Expose
        @ConfigOption(name = "Show Internal Name", desc = "Show internal names in item lore.")
        @ConfigEditorBoolean
        public boolean showInternalName = false;

        @Expose
        @ConfigOption(name = "Show Empty Internal Names", desc = "Shows internal name even for items with none.")
        @ConfigEditorBoolean
        public boolean showEmptyNames = false;

        @Expose
        @ConfigOption(name = "Show Item Rarity", desc = "Show item rarities in item lore.")
        @ConfigEditorBoolean
        public boolean showItemRarity = false;

        @Expose
        @ConfigOption(name = "Copy Internal Name", desc = "Copies the internal name of an item on key press in the clipboard.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int copyInternalName = Keyboard.KEY_NONE;

        @Expose
        @ConfigOption(name = "Show NPC Price", desc = "Show NPC price in item lore.")
        @ConfigEditorBoolean
        public boolean showNpcPrice = false;

        @Expose
        @ConfigOption(name = "Show Item UUID", desc = "Show the Unique Identifier of items in the lore.")
        @ConfigEditorBoolean
        public boolean showItemUuid = false;

        @Expose
        @ConfigOption(name = "Copy Item Data", desc = "Copies item NBT data on key press in a GUI to clipboard.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int copyItemData = Keyboard.KEY_NONE;

        @Expose
        @ConfigOption(name = "Copy Compressed Item Data", desc = "Copies compressed item NBT data on key press in a GUI to clipboard.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int copyItemDataCompressed = Keyboard.KEY_NONE;

        @Expose
        @ConfigOption(name = "Copy RNG Meter", desc = "Copies internal names and maxed XP needed from RNG meter inventories as json to clipboard.")
        @ConfigEditorBoolean
        public boolean copyRngMeter = false;

        @Expose
        @ConfigOption(name = "Copy Bestiary Data", desc = "Copies the bestiary data from the inventory as json to clipboard.")
        @ConfigEditorBoolean
        public boolean copyBestiaryData = false;

        @Expose
        @ConfigOption(name = "Highlight Missing Repo Items", desc = "Highlights each item in the current inventory that is not in your current NEU repo.")
        @ConfigEditorBoolean
        public boolean highlightMissingRepo = false;

        @Expose
        @ConfigOption(name = "Hot Swap Detection", desc = "Show chat messages when Hot Swap starts and ends.")
        @ConfigEditorBoolean
        public boolean hotSwapDetection = false;
    }

    @Expose
    @ConfigOption(name = "Slot Number", desc = "Show slot number in inventory while pressing this key.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int showSlotNumberKey = Keyboard.KEY_NONE;

    @ConfigOption(name = "Parkour Waypoints", desc = "")
    @Accordion
    @Expose
    public WaypointsConfig waypoint = new WaypointsConfig();

    public static class WaypointsConfig {

        @Expose
        @ConfigOption(name = "Save Hotkey", desc = "Saves block location to a temporarily parkour and copies everything to your clipboard.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int saveKey = Keyboard.KEY_NONE;

        @Expose
        @ConfigOption(name = "Delete Hotkey", desc = "Deletes the last saved location for when you make a mistake.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int deleteKey = Keyboard.KEY_NONE;

        @Expose
        @ConfigOption(name = "Show Platform Number", desc = "Show the index number over the platform for every parkour.")
        @ConfigEditorBoolean
        public boolean showPlatformNumber = false;

    }

    @Expose
    public Position debugPos = new Position(10, 10, false, true);

    @Expose
    public Position debugLocationPos = new Position(1, 160, false, true);

    @Expose
    @ConfigOption(name = "Minecraft Console", desc = "")
    @Accordion
    public MinecraftConsoleConfig minecraftConsoles = new MinecraftConsoleConfig();

    public static class MinecraftConsoleConfig {
        @Expose
        @ConfigOption(name = "Unfiltered Debug", desc = "Print the debug information for unfiltered console messages.")
        @ConfigEditorBoolean
        public boolean printUnfilteredDebugs = false;

        @Expose
        @ConfigOption(name = "Unfiltered Debug File", desc = "Print the debug information into log files instead of into the console for unfiltered console messages.")
        @ConfigEditorBoolean
        public boolean logUnfilteredFile = false;

        @Expose
        @ConfigOption(
                name = "Outside SkyBlock",
                desc = "Print the debug information for unfiltered console messages outside SkyBlock too."
        )
        @ConfigEditorBoolean
        public boolean printUnfilteredDebugsOutsideSkyBlock = false;

        @Expose
        @ConfigOption(
                name = "Log Filtered",
                desc = "Log the filtered messages into the console."
        )
        @ConfigEditorBoolean
        public boolean printFilteredReason = false;

        @Expose
        @ConfigOption(name = "Console Filters", desc = "")
        @Accordion
        public ConsoleFiltersConfig consoleFilter = new ConsoleFiltersConfig();

        public static class ConsoleFiltersConfig {
            @Expose
            @ConfigOption(name = "Filter Chat", desc = "Filter chat messages.")
            @ConfigEditorBoolean
            public boolean filterChat = false;

            @Expose
            @ConfigOption(name = "Filter Grow Buffer", desc = "Filter 'Needed to grow BufferBuilder buffer:'")
            @ConfigEditorBoolean
            public boolean filterGrowBuffer = true;

            @Expose
            @ConfigOption(name = "Filter Sound Error", desc = "Filter 'Unable to play unknown soundEvent'.")
            @ConfigEditorBoolean
            public boolean filterUnknownSound = true;

            @Expose
            @ConfigOption(name = "Filter Scoreboard Errors", desc = "Filter error messages with Scoreboard: removeTeam, createTeam, " +
                    "removeObjective and 'scoreboard team already exists'.")
            @ConfigEditorBoolean
            public boolean filterScoreboardErrors = true;

            @Expose
            @ConfigOption(name = "Filter Particle", desc = "Filter message 'Could not spawn particle effect VILLAGER_HAPPY'.")
            @ConfigEditorBoolean
            public boolean filterParticleVillagerHappy = true;

            @Expose
            @ConfigOption(name = "Filter OptiFine", desc = "Filter OptiFine messages CustomItems and ConnectedTextures during loading.")
            @ConfigEditorBoolean
            public boolean filterOptiFine = true;

            @Expose
            @ConfigOption(name = "Filter AsmHelper Transformer", desc = "Filter messages when AsmHelper is Transforming a class during loading.")
            @ConfigEditorBoolean
            public boolean filterAmsHelperTransformer = true;

            @Expose
            @ConfigOption(name = "Filter Applying AsmWriter", desc = "Filter messages when AsmHelper is applying AsmWriter ModifyWriter.")
            @ConfigEditorBoolean
            public boolean filterAsmHelperApplying = true;

            @Expose
            @ConfigOption(name = "Filter Biome ID Bounds", desc = "Filter message 'Biome ID is out of bounds'.")
            @ConfigEditorBoolean
            public boolean filterBiomeIdBounds = true;
        }
    }
}
