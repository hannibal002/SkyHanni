package at.hannibal2.skyhanni.config.features;

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
    @ConfigOption(name = "Green line", desc = "Adds green line around items that are clickable.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean hideNotClickableItemsGreenLine = true;

    @ConfigOption(name = "RNG Meter", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean rngMeter = false;

    @Expose
    @ConfigOption(name = "Floor Names", desc = "Show the floor names in the catacombs rng meter inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean rngMeterFloorName = false;

    @Expose
    @ConfigOption(name = "No Drop", desc = "Highlight floors without a drop selected in the catacombs rng meter inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean rngMeterNoDrop = false;

    @Expose
    @ConfigOption(name = "Selected Drop", desc = "Highlight the selected drop in the catacombs or slayer rng meter inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean rngMeterSelectedDrop = false;

    @ConfigOption(name = "Stats Tuning", desc = "")
    @ConfigEditorAccordion(id = 2)
    public boolean statsTuning = false;

    @Expose
    @ConfigOption(name = "Selected Stats", desc = "Show the tuning stats in the Thaumaturgy inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean statsTuningSelectedStats = true;

    @Expose
    @ConfigOption(name = "Tuning Points", desc = "Show the amount of selected tuning points in the stats tuning inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean statsTuningPoints = true;

    @Expose
    @ConfigOption(name = "Selected Template", desc = "Highlight the selected template in the stats tuning inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean statsTuningSelectedTemplate = true;

    @Expose
    @ConfigOption(name = "Template Stats", desc = "Show the type of stats for the tuning point templates.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean statsTuningTemplateStats = true;

    @Expose
    @ConfigOption(name = "Jacob Farming Contest", desc = "")
    @ConfigEditorAccordion(id = 3)
    public boolean jacobFarmingContest = false;

    @Expose
    @ConfigOption(name = "Unclaimed Rewards", desc = "Highlight contests with unclaimed rewards in the jacob inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean jacobFarmingContestHighlightRewards = true;

    @Expose
    @ConfigOption(name = "Duplicate Hider", desc = "Hides duplicate farming contests in the inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean jacobFarmingContestHideDuplicates = true;

    @Expose
    @ConfigOption(name = "Contest Time", desc = "Adds the real time format to the contest description.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean jacobFarmingContestRealTime = true;

    @Expose
    @ConfigOption(name = "Sack Items Display", desc = "")
    @Accordion
    public SackDisplay sackDisplay = new SackDisplay();

    public static class SackDisplay {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Show contained items inside a sack inventory.")
        @ConfigEditorBoolean
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
                "in larger gui scale, like the nether sack.)")
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
        @ConfigOption(name = "Show in Runes Sack", desc = "Show contained items inside a runes sack.")
        @ConfigEditorBoolean
        public boolean showRunes = false;

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
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Show Stacks", desc = "Show the item icon before name.")
        @ConfigEditorBoolean
        public boolean showStacks = true;

        @Expose
        @ConfigOption(name = "Name Length", desc = "Reduce item name length to gain extra space on screen.\n§cCalculated in pixels!")
        @ConfigEditorSlider(minStep = 1, minValue = 10, maxValue = 200)
        public int nameLength = 100;

        @Expose
        @ConfigOption(name = "Highlight slot", desc = "Highlight slot where the item is when you hover over it in the display.")
        @ConfigEditorBoolean
        public boolean enableHighlight = true;

        @Expose
        @ConfigOption(name = "Highlight color", desc = "Choose the highlight color.")
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
    @ConfigOption(
            name = "Item number",
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
                    "§bWishing Compass",
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
    public boolean displaySackName = false;

    @Expose
    @ConfigOption(name = "Anvil Combine Helper", desc = "Suggests the same item in the inventory when trying to combine two items in the anvil.")
    @ConfigEditorBoolean
    public boolean anvilCombineHelper = false;

    @Expose
    @ConfigOption(name = "Item Stars",
            desc = "Show a compact star count in the item name for all items.")
    @ConfigEditorBoolean
    public boolean itemStars = false;

    @Expose
    @ConfigOption(name = "Highlight Depleted Bonzo's Masks",
            desc = "Highlights used Bonzo's Masks with a background.")
    @ConfigEditorBoolean
    public boolean highlightDepletedBonzosMasks = false;

    @Expose
    @ConfigOption(name = "Missing Tasks",
            desc = "Highlight missing tasks in the SkyBlock level guide inventory.")
    @ConfigEditorBoolean
    public boolean highlightMissingSkyBlockLevelGuide = true;

    @Expose
    @ConfigOption(name = "Highlight Auctions",
            desc = "Highlight own items that are sold in green and that are expired in red.")
    @ConfigEditorBoolean
    public boolean highlightAuctions = true;


}