package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.features.inventory.chocolatefactory.CFConfig;
import at.hannibal2.skyhanni.config.features.inventory.customwardrobe.CustomWardrobeConfig;
import at.hannibal2.skyhanni.config.features.inventory.experimentationtable.ExperimentationTableConfig;
import at.hannibal2.skyhanni.config.features.inventory.helper.HelperConfig;
import at.hannibal2.skyhanni.config.features.inventory.sacks.OutsideSackValueConfig;
import at.hannibal2.skyhanni.config.features.itemability.ItemAbilityConfig;
import at.hannibal2.skyhanni.config.features.misc.EstimatedItemValueConfig;
import at.hannibal2.skyhanni.config.features.misc.PocketSackInASackConfig;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.LARVA_HOOK;
import static at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.NEW_YEAR_CAKE;
import static at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.RANCHERS_BOOTS_SPEED;
import static at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.VACUUM_GARDEN;

public class InventoryConfig {

    @Expose
    @Category(name = "SkyBlock Guide", desc = "Help find stuff to do in SkyBlock.")
    public SkyblockGuideConfig skyblockGuideConfig = new SkyblockGuideConfig();

    @Expose
    @Category(name = "Auction House", desc = "Be smart when buying or selling expensive items in the Auctions House.")
    public AuctionHouseConfig auctions = new AuctionHouseConfig();

    @Expose
    @Category(name = "Bazaar", desc = "Be smart when buying or selling many items in the Bazaar.")
    public BazaarConfig bazaar = new BazaarConfig();

    @Expose
    @Category(name = "Experimentation Table", desc = "QOL features for the Experimentation Table.")
    public ExperimentationTableConfig experimentationTable = new ExperimentationTableConfig();

    @Expose
    @Category(name = "Enchant Parsing", desc = "Settings for SkyHanni's Enchant Parsing")
    public EnchantParsingConfig enchantParsing = new EnchantParsingConfig();

    @Expose
    @Category(name = "Helpers", desc = "Some smaller Helper settings.")
    public HelperConfig helper = new HelperConfig();

    @Expose
    @Category(name = "Item Abilities", desc = "Stuff about item abilities.")
    public ItemAbilityConfig itemAbilities = new ItemAbilityConfig();

    @Expose
    @Category(name = "Custom Wardrobe", desc = "New Wardrobe Look.")
    public CustomWardrobeConfig customWardrobe = new CustomWardrobeConfig();

    @Expose
    @Category(name = "Chocolate Factory", desc = "Features to help you master the Chocolate Factory idle game.")
    public CFConfig chocolateFactory = new CFConfig();

    @Expose
    @ConfigOption(name = "Item Pickup Log", desc = "Logs all the picked up and dropped items")
    @Accordion
    // TODO remove the suffix "config"
    public ItemPickupLogConfig itemPickupLogConfig = new ItemPickupLogConfig();

    @Expose
    @Category(name = "Craftable Item List", desc = "Helps to find items to §e/craft.")
    @Accordion
    public CraftableItemListConfig craftableItemList = new CraftableItemListConfig();

    @Expose
    @ConfigOption(name = "Not Clickable Items", desc = "Better not click that item.")
    @Accordion
    public HideNotClickableConfig hideNotClickable = new HideNotClickableConfig();

    @Expose
    @ConfigOption(name = "Personal Compactor Overlay", desc = "Overlay for the Personal Compactor and Deletor.")
    @Accordion
    public PersonalCompactorConfig personalCompactor = new PersonalCompactorConfig();

    @Expose
    @ConfigOption(name = "Focus Mode", desc = "")
    @Accordion
    public FocusModeConfig focusMode = new FocusModeConfig();

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
    @ConfigOption(name = "Outside Sack Value", desc = "")
    @Accordion
    public OutsideSackValueConfig outsideSackValue = new OutsideSackValueConfig();

    @Expose
    @ConfigOption(name = "Estimated Item Value", desc = "(Prices for Enchantments, Reforge Stones, Gemstones, Drill Parts and more)")
    @Accordion
    public EstimatedItemValueConfig estimatedItemValues = new EstimatedItemValueConfig();

    @Expose
    @ConfigOption(name = "Chest Value", desc = "")
    @Accordion
    public ChestValueConfig chestValueConfig = new ChestValueConfig();

    @Expose
    @ConfigOption(name = "Get From Sack", desc = "")
    @Accordion
    public GetFromSackConfig gfs = new GetFromSackConfig();

    @Expose
    @ConfigOption(name = "Pocket Sack-In-A-Sack", desc = "")
    @Accordion
    public PocketSackInASackConfig pocketSackInASack = new PocketSackInASackConfig();

    @Expose
    @ConfigOption(name = "Page Scrolling", desc = "")
    @Accordion
    public PageScrollingConfig pageScrolling = new PageScrollingConfig();

    @Expose
    @ConfigOption(name = "New Year Cake Tracker", desc = "")
    @Accordion
    public CakeTrackerConfig cakeTracker = new CakeTrackerConfig();

    @Expose
    @ConfigOption(name = "Magical Power Display", desc = "")
    @Accordion
    public MagicalPowerConfig magicalPower = new MagicalPowerConfig();

    @Expose
    @ConfigOption(name = "Fann Cost Per XP/Bits", desc = "")
    @Accordion
    public FannCostConfig fannCost = new FannCostConfig();

    @Expose
    @ConfigOption(name = "Attribute Overlay", desc = "")
    @Accordion
    public AttributeOverlayConfig attributeOverlay = new AttributeOverlayConfig();

    @Expose
    @ConfigOption(name = "Attribute Shards", desc = "")
    @Accordion
    public AttributeShardsConfig attributeShards = new AttributeShardsConfig();

    @Expose
    @ConfigOption(name = "Evolving Items", desc = "")
    @Accordion
    @SearchTag("Time Pocket, Bottle of Jyrre, Dark Cacao Truffle, Discrite, Moby-Duck")
    public evolvingItemsConfig evolvingItems = new evolvingItemsConfig();

    @Expose
    @ConfigOption(name = "Trade Value", desc = "Creates a trade value overlay")
    @Accordion
    public TradeConfig trade = new TradeConfig();

    @Expose
    @ConfigOption(name = "Item Number", desc = "Showing the item number as a stack size for these items.")
    @ConfigEditorDraggableList
    @SearchTag("Time Pocket, Bottle of Jyrre, Dark Cacao Truffle, Discrite, Moby-Duck")
    public List<ItemNumberEntry> itemNumberAsStackSize = new ArrayList<>(Arrays.asList(
        NEW_YEAR_CAKE,
        RANCHERS_BOOTS_SPEED,
        LARVA_HOOK,
        VACUUM_GARDEN
    ));

    public enum ItemNumberEntry {
        MASTER_STAR_TIER("§bMaster Star Tier"),
        MASTER_SKULL_TIER("§bMaster Skull Tier"),
        DUNGEON_HEAD_FLOOR_NUMBER("§bDungeon Head Floor Number"),
        NEW_YEAR_CAKE("§bNew Year Cake"),
        PET_LEVEL("§bPet Level"),
        MINION_TIER("§bMinion Tier"),
        CRIMSON_ARMOR("§bCrimson Armor"),
        KUUDRA_KEY("§bKuudra Key"),
        SKILL_LEVEL("§bSkill Level"),
        COLLECTION_LEVEL("§bCollection Level"),
        RANCHERS_BOOTS_SPEED("§bRancher's Boots speed"),
        LARVA_HOOK("§bLarva Hook"),
        DUNGEON_POTION_LEVEL("§bDungeon Potion Level"),
        VACUUM_GARDEN("§bVacuum (Garden)"),
        EVOLVING_ITEMS("§bEvolving Items (Jyrre, Truffle, etc.)"),
        EDITION_NUMBER("§bEdition Number"),
        BINGO_GOAL_RANK("§bBingo Goal Rank"),
        SKYBLOCK_LEVEL("§bSkyblock Level"),
        BESTIARY_LEVEL("§bBestiary Level"),
        ;

        private final String displayName;

        ItemNumberEntry(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Expose
    @ConfigOption(name = "Highlight Widgets", desc = "Highlight enabled and disabled widgets in /tab.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightWidgets = true;

    @Expose
    @ConfigOption(name = " Vacuum Bag Cap", desc = "Cap the Garden Vacuum Bag item number display to 40.")
    @ConfigEditorBoolean
    public boolean vacuumBagCap = true;

    @Expose
    @ConfigOption(name = "Quick Craft Confirmation",
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
    @ConfigOption(name = "Anvil Combine Helper", desc = "Suggest the same item in the inventory when trying to combine two items in the anvil.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean anvilCombineHelper = false;

    @Expose
    @ConfigOption(name = "Item Stars", desc = "Show a compact star count in the item name for all items.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean itemStars = false;

    @Expose
    @ConfigOption(name = "Ultimate Enchant Star", desc = "Show a star on Enchanted Books with an Ultimate Enchant.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean ultimateEnchantStar = false;

    @Expose
    @ConfigOption(
        name = "Old SkyBlock Menu",
        desc = "Show old buttons in the SkyBlock Menu: Trade, Accessories, Potions, Quiver, Fishing and Sacks. " +
            "§cOnly works with the booster cookie effect active."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean oldSkyBlockMenu = false;

    @Expose
    @ConfigOption(name = "Missing Tasks", desc = "Highlight missing tasks in the SkyBlock Level Guide inventory.")
    // TODO move( , "inventory.highlightMissingSkyBlockLevelGuide", "inventory.skyblockGuideConfig.highlightMissingSkyBlockLevelGuide")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightMissingSkyBlockLevelGuide = true;

    @Expose
    @ConfigOption(name = "Power Stone Guide", desc = "Highlight missing power stones, show their total bazaar price, and allows to open the bazaar when clicking on the items in the Power Stone Guide.")
    // TODO move( , "inventory.powerStoneGuide", "inventory.skyblockGuideConfig.powerStoneGuide")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean powerStoneGuide = true;

    @Expose
    @ConfigOption(name = "Favorite Power Stone", desc = "Show your favorite power stones. You can add/remove them by shift clicking a Power Stone.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean favoritePowerStone = false;

    @Expose
    @ConfigOption(name = "Shift Click Equipment", desc = "Change normal clicks into shift clicks in equipment inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean shiftClickForEquipment = false;

    @Expose
    @ConfigOption(name = "Shift Click NPC sell", desc = "Change normal clicks to shift clicks in npc inventory for selling.")
    @ConfigEditorBoolean
    @FeatureToggle
    // TODO rename to shiftClickNpcSell
    public boolean shiftClickNPCSell = false;

    @Expose
    @ConfigOption(name = "Shift Click Brewing", desc = "Change normal clicks to shift clicks in Brewing Stand inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean shiftClickBrewing = false;

    @Expose
    @ConfigOption(name = "Stonk of Stonk Price", desc = "Show Price per Stonk when taking the minimum bid in Stonks Auction (Richard).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean stonkOfStonkPrice = true;

    @Expose
    @ConfigOption(name = "Minister in Calendar", desc = "Show the Minister with their perk in the Calendar.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean ministerInCalendar = true;

    @Expose
    @ConfigOption(name = "Show hex as actual color", desc = "Changes the color of hex codes to the actual color.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hexAsColorInLore = true;

    @Expose
    @ConfigOption(name = "Essence Shop Helper", desc = "Show extra information about remaining upgrades in essence shops.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean essenceShopHelper = true;

    @Expose
    @ConfigOption(name = "Snake Game Keybinds", desc = "Use WASD-Keys to move around in the Abiphone snake game.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean snakeGameKeybinds = true;

    @Expose
    @ConfigOption(name = "Highlight Active Beacon Effect", desc = "Highlights the currently selected beacon effect in the beacon inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightActiveBeaconEffect = true;

}
