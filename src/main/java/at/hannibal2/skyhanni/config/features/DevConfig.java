package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import org.lwjgl.input.Keyboard;

public class DevConfig {

    @Expose
    @ConfigOption(name = "Repo Auto Update", desc = "Update the repository on every startup.\n" +
            "§cOnly disable this if you know what you are doing!")
    @ConfigEditorBoolean
    public boolean repoAutoUpdate = true;

    @Expose
    @ConfigOption(name = "Debug", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean debugDO_NOT_USE = false;

    @Expose
    @ConfigOption(name = "Enable Debug", desc = "Enable Test logic")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean debugEnabled = false;

    @Expose
    @ConfigOption(name = "Command Logging", desc = "Logs stack trace information into the console when a command gets sent to Hypixel. (by any mod or the player)")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean commandLogs = false;

    @Expose
    @ConfigOption(
            name = "Mod Menu Log",
            desc = "Enables debug messages when the currently opened GUI changes, with the path to the gui class. " +
                    "Useful for adding more mods to quick mod menu switch."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean modMenuLog = false;

    @Expose
    @ConfigOption(name = "Show Internal Name", desc = "Show internal names in item lore.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean showInternalName = false;

    @Expose
    @ConfigOption(name = "Show Empty Internal Names", desc = "Shows internal name even for items with none.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean showEmptyNames = false;

    @Expose
    @ConfigOption(name = "Show Item Rarity", desc = "Show item rarities in item lore.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean showItemRarity = false;

    @Expose
    @ConfigOption(name = "Copy Internal Name", desc = "Copies the internal name of an item on key press in the clipboard.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    @ConfigAccordionId(id = 0)
    public int copyInternalName = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Show NPC Price", desc = "Show NPC price in item lore.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean showNpcPrice = false;

    @Expose
    @ConfigOption(name = "Show Item UUID", desc = "Show the Unique Identifier of items in the lore.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean showItemUuid = false;

    @Expose
    @ConfigOption(name = "Copy NBT Data", desc = "Copies compressed NBT data on key press in a GUI")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    @ConfigAccordionId(id = 0)
    public int copyNBTDataCompressed = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Copy RNG Meter", desc = "Copies internal names and maxed XP needed from RNG meter inventories as json to clipboard.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean copyRngMeter = false;

    @Expose
    @ConfigOption(name = "Copy Bestiary Data", desc = "Copies the bestiary data from the inventory as json to clipboard.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean copyBestiaryData = false;

    @Expose
    @ConfigOption(name = "Highlight Missing Repo Items", desc = "Highlights each item in the current inventory that is not in your current NEU repo.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean highlightMissingRepo = false;

    @Expose
    @ConfigOption(name = "Slot Number", desc = "Show slot number in inventory while pressing this key.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int showSlotNumberKey = Keyboard.KEY_NONE;

    @ConfigOption(name = "Parkour Waypoints", desc = "")
    @Accordion
    @Expose
    public Waypoints waypoint = new Waypoints();

    public static class Waypoints {

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

        @Expose
        @ConfigOption(name = "Outside SB", desc = "Make parkour waypoints outside of SkyBlock too.")
        @ConfigEditorBoolean
        public boolean parkourOutsideSB = false;

    }

    @Expose
    public Position debugPos = new Position(10, 10, false, true);

    @Expose
    public Position debugLocationPos = new Position(1, 160, false, true);

    @Expose
    @ConfigOption(name = "Minecraft Console", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean minecraftConsole = false;

    @Expose
    @ConfigOption(name = "Unfiltered Debug", desc = "Print the debug information for unfiltered console messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean printUnfilteredDebugs = false;

    @Expose
    @ConfigOption(name = "Unfiltered Debug File", desc = "Print the debug information into log files instead of into the console for unfiltered console messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean logUnfilteredFile = false;

    @Expose
    @ConfigOption(
            name = "Outside SkyBlock",
            desc = "Print the debug information for unfiltered console messages outside SkyBlock too."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean printUnfilteredDebugsOutsideSkyBlock = false;

    @Expose
    @ConfigOption(
            name = "Log Filtered",
            desc = "Log the filtered messages into the console."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean printFilteredReason = false;

    @Expose
    @ConfigOption(name = "Console Filters", desc = "")
    @ConfigAccordionId(id = 1)
    @ConfigEditorAccordion(id = 2)
    public boolean consoleFilters = false;

    @Expose
    @ConfigOption(name = "Filter Chat", desc = "Filter chat messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterChat = false;

    @Expose
    @ConfigOption(name = "Filter Grow Buffer", desc = "Filter 'Needed to grow BufferBuilder buffer:'")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterGrowBuffer = true;

    @Expose
    @ConfigOption(name = "Filter Sound Error", desc = "Filter 'Unable to play unknown soundEvent'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterUnknownSound = true;

    @Expose
    @ConfigOption(name = "Filter Scoreboard Errors", desc = "Filter error messages with Scoreboard: removeTeam, createTeam, " +
            "removeObjective and 'scoreboard team already exists'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterScoreboardErrors = true;

    @Expose
    @ConfigOption(name = "Filter Particle", desc = "Filter message 'Could not spawn particle effect VILLAGER_HAPPY'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterParticleVillagerHappy = true;

    @Expose
    @ConfigOption(name = "Filter OptiFine", desc = "Filter OptiFine messages CustomItems and ConnectedTextures during loading.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterOptiFine = true;

    @Expose
    @ConfigOption(name = "Filter AsmHelper Transformer", desc = "Filter messages when AsmHelper is Transforming a class during loading.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterAmsHelperTransformer = true;

    @Expose
    @ConfigOption(name = "Filter Applying AsmWriter", desc = "Filter messages when AsmHelper is applying AsmWriter ModifyWriter.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterAsmHelperApplying = true;

    @Expose
    @ConfigOption(name = "Filter Biome ID Bounds", desc = "Filter message 'Biome ID is out of bounds'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterBiomeIdBounds = true;
}
