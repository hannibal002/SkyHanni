package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.HasLegacyId;
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

import static at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.LARVA_HOOK;
import static at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.NEW_YEAR_CAKE;
import static at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.RANCHERS_BOOTS_SPEED;
import static at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.VACUUM_GARDEN;

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
    @ConfigEditorDraggableList()
    public List<ItemNumberEntry> itemNumberAsStackSize = new ArrayList<>(Arrays.asList(
        NEW_YEAR_CAKE,
        RANCHERS_BOOTS_SPEED,
        LARVA_HOOK,
        VACUUM_GARDEN
    ));

    public enum ItemNumberEntry implements HasLegacyId {
        MASTER_STAR_TIER("§bMaster Star Tier", 0),
        MASTER_SKULL_TIER("§bMaster Skull Tier", 1),
        DUNGEON_HEAD_FLOOR_NUMBER("§bDungeon Head Floor Number", 2),
        NEW_YEAR_CAKE("§bNew Year Cake", 3),
        PET_LEVEL("§bPet Level", 4),
        MINION_TIER("§bMinion Tier", 5),
        CRIMSON_ARMOR("§bCrimson Armor", 6),
        REMOVED("§7(Removed)", 7),
        KUUDRA_KEY("§bKuudra Key", 8),
        SKILL_LEVEL("§bSkill Level", 9),
        COLLECTION_LEVEL("§bCollection Level", 10),
        RANCHERS_BOOTS_SPEED("§bRancher's Boots speed", 11),
        LARVA_HOOK("§bLarva Hook", 12),
        DUNGEON_POTION_LEVEL("§bDungeon Potion Level", 13),
        VACUUM_GARDEN("§bVacuum (Garden)", 14),
        BOTTLE_OF_JYRRE("§bBottle Of Jyrre", 15),
        EDITION_NUMBER("§bEdition Number", 16),
        ;

        private final String str;
        private final int legacyId;

        ItemNumberEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        ItemNumberEntry(String str) {
            this(str, -1);
        }

        @Override
        public int getLegacyId() {
            return legacyId;
        }

        @Override
        public String toString() {
            return str;
        }
    }

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
