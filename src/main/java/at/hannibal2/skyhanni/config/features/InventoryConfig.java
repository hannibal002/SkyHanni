package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InventoryConfig {

    @Expose
    @ConfigOption(name = "Not Clickable Items", desc = "")
    @Accordion
    public HideNotClickableConfig hideNotClickable = new HideNotClickableConfig();

    public static class HideNotClickableConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Hide items that are not clickable in the current inventory: ah, bz, accessory bag, etc.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean items = false;

        @Expose
        @ConfigOption(name = "Block Clicks", desc = "Block the clicks on these items.")
        @ConfigEditorBoolean
        public boolean itemsBlockClicks = true;

        @Expose
        @ConfigOption(
            name = "Opacity",
            desc = "How strong should the items be grayed out?"
        )
        @ConfigEditorSlider(
            minValue = 0,
            maxValue = 255,
            minStep = 5
        )
        public int opacity = 180;

        @Expose
        @ConfigOption(name = "Bypass With Control", desc = "Adds the ability to bypass not clickable items when holding the control key.")
        @ConfigEditorBoolean
        public boolean itemsBypass = true;

        @Expose
        @ConfigOption(name = "Green Line", desc = "Adds green line around items that are clickable.")
        @ConfigEditorBoolean
        public boolean itemsGreenLine = true;

    }

    @Expose
    @ConfigOption(name = "RNG Meter", desc = "")
    @Accordion
    public RngMeterConfig rngMeter = new RngMeterConfig();

    public static class RngMeterConfig {
        @Expose
        @ConfigOption(name = "Floor Names", desc = "Show the Floor names in the Catacombs RNG Meter inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean floorName = false;

        @Expose
        @ConfigOption(name = "No Drop", desc = "Highlight floors without a drop selected in the Catacombs RNG Meter inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean noDrop = false;

        @Expose
        @ConfigOption(name = "Selected Drop", desc = "Highlight the selected drop in the Catacombs or Slayer RNG Meter inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean selectedDrop = false;
    }

    @Expose
    @ConfigOption(name = "Stats Tuning", desc = "")
    @Accordion
    public StatsTuningConfig statsTuning = new StatsTuningConfig();

    public static class StatsTuningConfig {
        @Expose
        @ConfigOption(name = "Selected Stats", desc = "Show the tuning stats in the Thaumaturgy inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean selectedStats = true;

        @Expose
        @ConfigOption(name = "Tuning Points", desc = "Show the amount of selected Tuning Points in the Stats Tuning inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean points = true;

        @Expose
        @ConfigOption(name = "Selected Template", desc = "Highlight the selected template in the Stats Tuning inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean selectedTemplate = true;

        @Expose
        @ConfigOption(name = "Template Stats", desc = "Show the type of stats for the Tuning Point templates.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean templateStats = true;
    }

    @Expose
    @ConfigOption(name = "Jacob Farming Contest", desc = "")
    @Accordion
    public JacobFarmingContestConfig jacobFarmingContests = new JacobFarmingContestConfig();

    public static class JacobFarmingContestConfig {
        @Expose
        @ConfigOption(name = "Unclaimed Rewards", desc = "Highlight contests with unclaimed rewards in the Jacob inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean highlightRewards = true;

        @Expose
        @ConfigOption(name = "Contest Time", desc = "Adds the real time format to the Contest description.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean realTime = true;

        @Expose
        @ConfigOption(name = "Medal Icon", desc = "Adds a symbol that shows what medal you received in this Contest. " +
            "§eIf you use a texture pack this may cause conflicting icons.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean medalIcon = true;

        @Expose
        @ConfigOption(name = "Finnegan Icon", desc = "Uses a different indicator for when the Contest happened during Mayor Finnegan.")
        @ConfigEditorBoolean
        public boolean finneganIcon = true;
    }


    @Expose
    @ConfigOption(name = "Sack Items Display", desc = "")
    @Accordion
    public SackDisplayConfig sackDisplay = new SackDisplayConfig();

    public static class SackDisplayConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Show contained items inside a sack inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(
            name = "Highlight Full",
            desc = "Highlight items that are full in red.\n" +
                "§eDoes not need the option above to be enabled."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean highlightFull = true;

        @Expose
        @ConfigOption(name = "Number Format", desc = "Either show Default, Formatted or Unformatted numbers.\n" +
            "§eDefault: §72,240/2.2k\n" +
            "§eFormatted: §72.2k/2.2k\n" +
            "§eUnformatted: §72,240/2,200")
        @ConfigEditorDropdown(values = {"Default", "Formatted", "Unformatted"})
        public int numberFormat = 1;

        @Expose
        @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
        @ConfigEditorSlider(
            minValue = 0,
            maxValue = 10,
            minStep = 1)
        public int extraSpace = 1;

        @Expose
        @ConfigOption(name = "Sorting Type", desc = "Sorting type of items in sack.")
        @ConfigEditorDropdown(values = {"Descending (Stored)", "Ascending (Stored)", "Descending (Price)", "Ascending (Price)"})
        public int sortingType = 0;

        @Expose
        @ConfigOption(name = "Item To Show", desc = "Choose how many items are displayed. (Some sacks have too many items to fit\n" +
            "in larger GUI scales, like the nether sack.)")
        @ConfigEditorSlider(
            minValue = 0,
            maxValue = 45,
            minStep = 1
        )
        public int itemToShow = 15;

        @Expose
        @ConfigOption(name = "Show Empty Item", desc = "Show empty item quantity in the display.")
        @ConfigEditorBoolean
        public boolean showEmpty = true;

        @Expose
        @ConfigOption(name = "Show Price", desc = "Show price for each item in sack.")
        @ConfigEditorBoolean
        public boolean showPrice = true;

        @Expose
        @ConfigOption(name = "Price Format", desc = "Format of the price displayed.\n" +
            "§eFormatted: §7(12k)\n" +
            "§eUnformatted: §7(12,421)")
        @ConfigEditorDropdown(values = {"Formatted", "Unformatted"})
        public int priceFormat = 0;

        @Expose
        @ConfigOption(name = "Show Price From", desc = "Show price from Bazaar or NPC.")
        @ConfigEditorDropdown(values = {"Bazaar", "NPC"})
        public int priceFrom = 0;

        @Expose
        public Position position = new Position(144, 139, false, true);
    }

    @Expose
    @ConfigOption(name = "Chest Value", desc = "")
    @Accordion
    public ChestValueConfig chestValueConfig = new ChestValueConfig();

    public static class ChestValueConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Enable estimated value of chest.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Enabled in dungeons", desc = "Enable the feature in dungeons.")
        @ConfigEditorBoolean
        public boolean enableInDungeons = false;

        @Expose
        @ConfigOption(name = "Enable during Item Value", desc = "Show this display even if the Estimated Item Value is visible.")
        @ConfigEditorBoolean
        public boolean showDuringEstimatedItemValue = false;

        @Expose
        @ConfigOption(name = "Show Stacks", desc = "Show the item icon before name.")
        @ConfigEditorBoolean
        public boolean showStacks = true;

        @Expose
        @ConfigOption(name = "Display Type", desc = "Try to align everything to look nicer.")
        @ConfigEditorBoolean
        public boolean alignedDisplay = true;

        @Expose
        @ConfigOption(name = "Name Length", desc = "Reduce item name length to gain extra space on screen.\n§cCalculated in pixels!")
        @ConfigEditorSlider(minStep = 1, minValue = 100, maxValue = 150)
        public int nameLength = 100;

        @Expose
        @ConfigOption(name = "Highlight Slot", desc = "Highlight slot where the item is when you hover over it in the display.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enableHighlight = true;

        @Expose
        @ConfigOption(name = "Highlight Color", desc = "Choose the highlight color.")
        @ConfigEditorColour
        public String highlightColor = "0:249:0:255:88";

        @Expose
        @ConfigOption(name = "Sorting Type", desc = "Price sorting type.")
        @ConfigEditorDropdown(values = {"Descending", "Ascending"})
        public int sortingType = 0;

        @Expose
        @ConfigOption(name = "Value formatting Type", desc = "Format of the price.")
        @ConfigEditorDropdown(values = {"Short", "Long"})
        public int formatType = 0;

        @Expose
        @ConfigOption(name = "Item To Show", desc = "Choose how many items are displayed.\n" +
            "All items in the chest are still counted for the total value.")
        @ConfigEditorSlider(
            minValue = 0,
            maxValue = 54,
            minStep = 1
        )
        public int itemToShow = 15;

        @Expose
        @ConfigOption(name = "Hide below", desc = "Item item value below configured amount.\n" +
            "Items are still counted for the total value.")
        @ConfigEditorSlider(
            minValue = 50_000,
            maxValue = 10_000_000,
            minStep = 50_000
        )
        public int hideBelow = 100_000;


        @Expose
        public Position position = new Position(107, 141, false, true);
    }

    @Expose
    @ConfigOption(name = "Helper", desc = "")
    @Accordion
    public HelperConfig helper = new HelperConfig();

    public static class HelperConfig {
        @Expose
        @ConfigOption(name = "Melody's Hair Harp", desc = "")
        @Accordion
        public HarpConfig harp = new HarpConfig();

        public static class HarpConfig {
            @Expose
            @ConfigOption(name = "Use Keybinds", desc = "In the Harp, press buttons with your number row on the keyboard instead of clicking.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean keybinds = false;

            @Expose
            @ConfigOption(name = "Show Numbers", desc = "In the Harp, show buttons as stack size (intended to be used with the Keybinds).")
            @ConfigEditorBoolean
            public boolean showNumbers = false;

            @Expose
            @ConfigOption(name = "Keybinds", desc = "")
            @Accordion
            public HarpConfigKeyBinds harpKeybinds = new HarpConfigKeyBinds();

            public static class HarpConfigKeyBinds {
                @Expose
                @ConfigOption(name = "Key 1", desc = "Key for the first Node")
                @ConfigEditorKeybind(defaultKey = Keyboard.KEY_1)
                public int key1 = Keyboard.KEY_1;
                @Expose
                @ConfigOption(name = "Key 2", desc = "Key for the second Node")
                @ConfigEditorKeybind(defaultKey = Keyboard.KEY_2)
                public int key2 = Keyboard.KEY_2;
                @Expose
                @ConfigOption(name = "Key 3", desc = "Key for the third Node")
                @ConfigEditorKeybind(defaultKey = Keyboard.KEY_3)
                public int key3 = Keyboard.KEY_3;
                @Expose
                @ConfigOption(name = "Key 4", desc = "Key for the fourth Node")
                @ConfigEditorKeybind(defaultKey = Keyboard.KEY_4)
                public int key4 = Keyboard.KEY_4;
                @Expose
                @ConfigOption(name = "Key 5", desc = "Key for the fifth Node")
                @ConfigEditorKeybind(defaultKey = Keyboard.KEY_5)
                public int key5 = Keyboard.KEY_5;
                @Expose
                @ConfigOption(name = "Key 6", desc = "Key for the sixth Node")
                @ConfigEditorKeybind(defaultKey = Keyboard.KEY_6)
                public int key6 = Keyboard.KEY_6;
                @Expose
                @ConfigOption(name = "Key 7", desc = "Key for the seventh Node")
                @ConfigEditorKeybind(defaultKey = Keyboard.KEY_7)
                public int key7 = Keyboard.KEY_7;
            }
        }

        @Expose
        @ConfigOption(name = "Tia Relay Abiphone Network Maintenance", desc = "")
        @Accordion
        public TiaRelayConfig tiaRelay = new TiaRelayConfig();

        public static class TiaRelayConfig {

            @Expose
            @ConfigOption(name = "Sound Puzzle Helper", desc = "Helps with solving the sound puzzle for Tia (The 9 Operator Chips to do maintainance for the Abiphone Network).")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean soundHelper = true;

            @Expose
            @ConfigOption(name = "Next Waypoint", desc = "Show the next relay waypoint for Tia the Fairy, where maintenance for the Abiphone network needs to be done.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean nextWaypoint = true;

            @Expose
            @ConfigOption(name = "All Waypoints", desc = "Show all relay waypoints at once (intended for debugging).")
            @ConfigEditorBoolean
            public boolean allWaypoints = false;

            @Expose
            @ConfigOption(name = "Mute Sound", desc = "Mutes the sound when close to the relay.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean tiaRelayMute = true;
        }
    }

    @Expose
    @ConfigOption(
        name = "Item Number",
        desc = "Showing the item number as a stack size for these items."
    )
    @ConfigEditorDraggableList(
        exampleText = {
            "§bMaster Star Tier",
            "§bMaster Skull Tier",
            "§bDungeon Head Floor Number",
            "§bNew Year Cake",
            "§bPet Level",
            "§bMinion Tier",
            "§bCrimson Armor",
            "§7(Removed)",
            "§bKuudra Key",
            "§bSkill Level",
            "§bCollection Level",
            "§bRancher's Boots speed",
            "§bLarva Hook",
            "§bDungeon Potion Level"
        }
    )
    public List<Integer> itemNumberAsStackSize = new ArrayList<>(Arrays.asList(3, 9, 11, 12));

    @Expose
    @ConfigOption(
        name = "Quick Craft Confirmation",
        desc = "Require Ctrl+Click to craft items that aren't often quick crafted " +
            "(e.g. armor, weapons, accessories). Sack items can be crafted normally."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean quickCraftingConfirmation = false;

    @Expose
    @ConfigOption(name = "Sack Name", desc = "Show an abbreviation of the sack name.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean displaySackName = false;

    @Expose
    @ConfigOption(name = "Anvil Combine Helper", desc = "Suggests the same item in the inventory when trying to combine two items in the anvil.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean anvilCombineHelper = false;

    @Expose
    @ConfigOption(name = "Item Stars",
        desc = "Show a compact star count in the item name for all items.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean itemStars = false;

    @Expose
    @ConfigOption(name = "Missing Tasks",
        desc = "Highlight missing tasks in the SkyBlock Level Guide inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightMissingSkyBlockLevelGuide = true;

    @Expose
    @ConfigOption(name = "Highlight Auctions",
        desc = "Highlight own items that are sold in green and that are expired in red.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightAuctions = true;

    @Expose
    @ConfigOption(name = "Shift Click Equipment", desc = "Makes normal clicks to shift clicks in equipment inventory")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean shiftClickForEquipment = false;

}
