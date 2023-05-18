package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Inventory {

    @ConfigOption(name = "Not Clickable Items", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean hideNotClickable = false;

    @Expose
    @ConfigOption(name = "Not Clickable Items Enabled", desc = "Hide items that are not clickable in the current inventory: ah, bz, accessory bag, etc.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean hideNotClickableItems = false;

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


    @ConfigOption(name = "Sack Items Display", desc = "")
    @ConfigEditorAccordion(id = 4)
    public boolean sackDisplay = false;

    @Expose
    @ConfigOption(name = "Sack Items Display Enabled", desc = "Show contained items inside a sack inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean showSackDisplay = true;

    @Expose
    @ConfigOption(name = "Number Format", desc = "Either show Default, Formatted or Unformatted numbers.\n" +
            "§eDefault: §72,240/2.2k\n" +
            "§eFormatted: §72.2k/2.2k\n" +
            "§eUnformatted: §72,240/2,200")
    @ConfigEditorDropdown(values = {"Default", "Formatted", "Unformatted"})
    @ConfigAccordionId(id = 4)
    public int showFullNumber = 1;

    @Expose
    @ConfigOption(name = "Sorting Type", desc = "Either show full number or abbreviated. \n" +
            "§eDefault: §7Same as displayed in sacks\n" +
            "§eFormatted: §72.2k/2.2k\n" +
            "§eUnformatted: §72,240/2,200")
    @ConfigEditorDropdown(values = {"Descending", "Ascending"})
    @ConfigAccordionId(id = 4)
    public int sortingType = 0;

    @Expose
    @ConfigOption(name = "Show in Runes Sack", desc = "Show contained items inside a runes sack.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean showRuneSackDisplay = false;

    @Expose
    public Position sackDisplayPosition = new Position(155, -57, false, true);

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
                    "§bRancher's Boots speed"
            }
    )
    public List<Integer> itemNumberAsStackSize = new ArrayList<>(Arrays.asList(3, 9, 11));

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
