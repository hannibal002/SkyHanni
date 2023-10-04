package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigAccordionId;
import io.github.moulberry.moulconfig.annotations.ConfigEditorAccordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InventoryConfig {

    @ConfigOption(name = "Not Clickable Items", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean hideNotClickable = false;

    @Expose
    @ConfigOption(name = "Enabled", desc = "Hide items that are not clickable in the current inventory: ah, bz, accessory bag, etc.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean hideNotClickableItems = false;

    @Expose
    @ConfigOption(name = "Block Clicks", desc = "Block the clicks on these items.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean hideNotClickableItemsBlockClicks = true;

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
    @ConfigAccordionId(id = 0)
    public int hideNotClickableOpacity = 180;

    @Expose
    @ConfigOption(name = "Bypass With Control", desc = "Adds the ability to bypass not clickable items when holding the control key.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean notClickableItemsBypass = true;

    @Expose
    @ConfigOption(name = "Green Line", desc = "Adds green line around items that are clickable.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean hideNotClickableItemsGreenLine = true;

    @ConfigOption(name = "RNG Meter", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean rngMeter = false;

    @Expose
    @ConfigOption(name = "Floor Names", desc = "Show the Floor names in the Catacombs RNG Meter inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean rngMeterFloorName = false;

    @Expose
    @ConfigOption(name = "No Drop", desc = "Highlight floors without a drop selected in the Catacombs RNG Meter inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean rngMeterNoDrop = false;

    @Expose
    @ConfigOption(name = "Selected Drop", desc = "Highlight the selected drop in the Catacombs or Slayer RNG Meter inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean rngMeterSelectedDrop = false;

    @ConfigOption(name = "Stats Tuning", desc = "")
    @ConfigEditorAccordion(id = 2)
    public boolean statsTuning = false;

    @Expose
    @ConfigOption(name = "Selected Stats", desc = "Show the tuning stats in the Thaumaturgy inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    @FeatureToggle
    public boolean statsTuningSelectedStats = true;

    @Expose
    @ConfigOption(name = "Tuning Points", desc = "Show the amount of selected Tuning Points in the Stats Tuning inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    @FeatureToggle
    public boolean statsTuningPoints = true;

    @Expose
    @ConfigOption(name = "Selected Template", desc = "Highlight the selected template in the Stats Tuning inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    @FeatureToggle
    public boolean statsTuningSelectedTemplate = true;

    @Expose
    @ConfigOption(name = "Template Stats", desc = "Show the type of stats for the Tuning Point templates.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    @FeatureToggle
    public boolean statsTuningTemplateStats = true;

    @Expose
    @ConfigOption(name = "Jacob Farming Contest", desc = "")
    @ConfigEditorAccordion(id = 3)
    public boolean jacobFarmingContest = false;

    @Expose
    @ConfigOption(name = "Unclaimed Rewards", desc = "Highlight contests with unclaimed rewards in the Jacob inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean jacobFarmingContestHighlightRewards = true;

    @Expose
    @ConfigOption(name = "Contest Time", desc = "Adds the real time format to the Contest description.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean jacobFarmingContestRealTime = true;

    @Expose
    @ConfigOption(name = "Medal Icon", desc = "Adds a symbol that shows what medal you received in this Contest. " +
            "§eIf you use a texture pack this may cause conflicting icons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean jacobFarmingContestMedalIcon = true;

    @Expose
    @ConfigOption(name = "Finnegan Icon", desc = "Uses a different indicator for when the Contest happened during Mayor Finnegan.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean jacobFarmingContestFinneganIcon = true;

    @Expose
    @ConfigOption(name = "Sack Items Display", desc = "")
    @Accordion
    public SackDisplay sackDisplay = new SackDisplay();

    public static class SackDisplay {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Show contained items inside a sack inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

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
        @ConfigOption(name = "Enabled", desc = "Enabled estimated value of chest")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

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


}
