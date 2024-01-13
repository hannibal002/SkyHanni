package at.hannibal2.skyhanni.config.features.dev;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class DebugConfig {
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

    @Expose
    @ConfigOption(name = "SkyHanni Event Counter", desc = "Count once per second how many skyhanni events gets triggered, " +
        "show the total amount in console output.")
    @ConfigEditorBoolean
    public boolean eventCounter = false;

    @Expose
    @ConfigOption(name = "Bypass Advanced Tab List", desc = "The Advaced Player Tab list is disabled whie pressing this hotkey.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int bypassAdvancedPlayerTabList = Keyboard.KEY_NONE;
}
