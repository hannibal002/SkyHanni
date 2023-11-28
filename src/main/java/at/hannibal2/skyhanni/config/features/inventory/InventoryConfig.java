package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.features.inventory.helper.HelperConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.Category;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InventoryConfig {

    @Expose
    @ConfigOption(name = "Not Clickable Items", desc = "")
    @Accordion
    public HideNotClickableConfig hideNotClickable = new HideNotClickableConfig();

    @Expose
    @ConfigOption(name = "RNG Meter", desc = "")
    @Accordion
    public RngMeterConfig rngMeter = new RngMeterConfig();

    @Expose
    @ConfigOption(name = "Stats Tuning", desc = "")
    @Accordion
    public StatsTuningConfig statsTuning = new StatsTuningConfig();

    @Expose
    @ConfigOption(name = "Jacob Farming Contest", desc = "")
    @Accordion
    public JacobFarmingContestConfig jacobFarmingContests = new JacobFarmingContestConfig();


    @Expose
    @ConfigOption(name = "Sack Items Display", desc = "")
    @Accordion
    public SackDisplayConfig sackDisplay = new SackDisplayConfig();

    @Expose
    @ConfigOption(name = "Chest Value", desc = "")
    @Accordion
    public ChestValueConfig chestValueConfig = new ChestValueConfig();

    @Expose
    @Category(name = "Helpers", desc = "Settings for Helpers")
    public HelperConfig helper = new HelperConfig();

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
            "§bDungeon Potion Level",
            "§bVacuum (Garden)",
            "§bBottle Of Jyrre",
            "§bEdition Number"
        }
    )
    public List<Integer> itemNumberAsStackSize = new ArrayList<>(Arrays.asList(3, 11, 12, 14));

    @Expose
    @ConfigOption(name = " Vacuum Bag Cap", desc = "Capping the Garden Vacuum Bag item number display to 40.")
    @ConfigEditorBoolean
    public boolean vacuumBagCap = true;

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
