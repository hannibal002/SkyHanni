package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.features.inventory.chocolatefactory.CFConfig
import at.hannibal2.skyhanni.config.features.inventory.customwardrobe.CustomWardrobeConfig
import at.hannibal2.skyhanni.config.features.inventory.experimentationtable.ExperimentationTableConfig
import at.hannibal2.skyhanni.config.features.inventory.helper.HelperConfig
import at.hannibal2.skyhanni.config.features.inventory.sacks.OutsideSackValueConfig
import at.hannibal2.skyhanni.config.features.itemability.ItemAbilityConfig
import at.hannibal2.skyhanni.config.features.misc.EstimatedItemValueConfig
import at.hannibal2.skyhanni.config.features.misc.PocketSackInASackConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class InventoryConfig {
    @Expose
    @Category(name = "SkyBlock Guide", desc = "Help find stuff to do in SkyBlock.")
    val skyblockGuide: SkyblockGuideConfig = SkyblockGuideConfig()

    @Expose
    @Category(name = "Auction House", desc = "Be smart when buying or selling expensive items in the Auctions House.")
    val auctions: AuctionHouseConfig = AuctionHouseConfig()

    @Expose
    @Category(name = "Bazaar", desc = "Be smart when buying or selling many items in the Bazaar.")
    val bazaar: BazaarConfig = BazaarConfig()

    @Expose
    @Category(name = "Experimentation Table", desc = "QOL features for the Experimentation Table.")
    val experimentationTable: ExperimentationTableConfig = ExperimentationTableConfig()

    @Expose
    @Category(name = "Enchant Parsing", desc = "Settings for SkyHanni's Enchant Parsing")
    val enchantParsing: EnchantParsingConfig = EnchantParsingConfig()

    @Expose
    @Category(name = "Helpers", desc = "Some smaller Helper settings.")
    val helper: HelperConfig = HelperConfig()

    @Expose
    @Category(name = "Item Abilities", desc = "Stuff about item abilities.")
    val itemAbilities: ItemAbilityConfig = ItemAbilityConfig()

    @Expose
    @Category(name = "Custom Wardrobe", desc = "New Wardrobe Look.")
    val customWardrobe: CustomWardrobeConfig = CustomWardrobeConfig()

    @Expose
    @Category(name = "Chocolate Factory", desc = "Features to help you master the Chocolate Factory idle game.")
    val chocolateFactory: CFConfig = CFConfig()

    @Expose
    @ConfigOption(name = "Improved SB Menus", desc = "")
    @Accordion
    val improvedSBMenus: ImprovedSBMenusConfig = ImprovedSBMenusConfig()

    @Expose
    @ConfigOption(name = "Item Pickup Log", desc = "Logs all the picked up and dropped items")
    @Accordion
    val itemPickupLog: ItemPickupLogConfig = ItemPickupLogConfig()

    @Expose
    @Category(name = "Craftable Item List", desc = "Helps to find items to §e/craft.")
    @Accordion
    val craftableItemList: CraftableItemListConfig = CraftableItemListConfig()

    @Expose
    @ConfigOption(name = "Not Clickable Items", desc = "Better not click that item.")
    @Accordion
    val hideNotClickable: HideNotClickableConfig = HideNotClickableConfig()

    @Expose
    @ConfigOption(name = "Personal Compactor Overlay", desc = "Overlay for the Personal Compactor and Deletor.")
    @Accordion
    val personalCompactor: PersonalCompactorConfig = PersonalCompactorConfig()

    @Expose
    @ConfigOption(name = "Focus Mode", desc = "")
    @Accordion
    val focusMode: FocusModeConfig = FocusModeConfig()

    @Expose
    @ConfigOption(name = "RNG Meter", desc = "")
    @Accordion
    val rngMeter: RngMeterConfig = RngMeterConfig()

    @Expose
    @ConfigOption(name = "Stats Tuning", desc = "")
    @Accordion
    val statsTuning: StatsTuningConfig = StatsTuningConfig()

    @Expose
    @ConfigOption(name = "Jacob Farming Contest", desc = "")
    @Accordion
    val jacobFarmingContests: JacobFarmingContestConfig = JacobFarmingContestConfig()

    @Expose
    @ConfigOption(name = "Sack Items Display", desc = "")
    @Accordion
    val sackDisplay: SackDisplayConfig = SackDisplayConfig()

    @Expose
    @ConfigOption(name = "Outside Sack Value", desc = "")
    @Accordion
    val outsideSackValue: OutsideSackValueConfig = OutsideSackValueConfig()

    @Expose
    @ConfigOption(
        name = "Estimated Item Value",
        desc = "(Prices for Enchantments, Reforge Stones, Gemstones, Drill Parts and more)",
    )
    @Accordion
    val estimatedItemValues: EstimatedItemValueConfig = EstimatedItemValueConfig()

    @Expose
    @ConfigOption(name = "Chest Value", desc = "")
    @Accordion
    val chestValueConfig: ChestValueConfig = ChestValueConfig()

    @Expose
    @ConfigOption(name = "Get From Sack", desc = "")
    @Accordion
    val gfs: GetFromSackConfig = GetFromSackConfig()

    @Expose
    @ConfigOption(name = "Pocket Sack-In-A-Sack", desc = "")
    @Accordion
    val pocketSackInASack: PocketSackInASackConfig = PocketSackInASackConfig()

    @Expose
    @ConfigOption(name = "Page Scrolling", desc = "")
    @Accordion
    val pageScrolling: PageScrollingConfig = PageScrollingConfig()

    @Expose
    @ConfigOption(name = "New Year Cake Tracker", desc = "")
    @Accordion
    val cakeTracker: CakeTrackerConfig = CakeTrackerConfig()

    @Expose
    @ConfigOption(name = "Magical Power Display", desc = "")
    @Accordion
    val magicalPower: MagicalPowerConfig = MagicalPowerConfig()

    @Expose
    @ConfigOption(name = "Fann Cost Per XP/Bits", desc = "")
    @Accordion
    val fannCost: FannCostConfig = FannCostConfig()

    @Expose
    @ConfigOption(name = "Attribute Overlay", desc = "")
    @Accordion
    val attributeOverlay: AttributeOverlayConfig = AttributeOverlayConfig()

    @Expose
    @ConfigOption(name = "Attribute Shards", desc = "")
    @Accordion
    val attributeShards: AttributeShardsConfig = AttributeShardsConfig()

    @Expose
    @ConfigOption(name = "Evolving Items", desc = "")
    @Accordion
    @SearchTag("Time Pocket, Bottle of Jyrre, Dark Cacao Truffle, Discrite, Moby-Duck")
    val evolvingItems: EvolvingItemsConfig = EvolvingItemsConfig()

    @Expose
    @ConfigOption(name = "Trade Value", desc = "Creates a trade value overlay")
    @Accordion
    val trade: TradeConfig = TradeConfig()

    @Expose
    @ConfigOption(name = "Item Number", desc = "Showing the item number as a stack size for these items.")
    @ConfigEditorDraggableList
    @SearchTag("Time Pocket, Bottle of Jyrre, Dark Cacao Truffle, Discrite, Moby-Duck")
    val itemNumberAsStackSize: MutableList<ItemNumberEntry> = mutableListOf(
        ItemNumberEntry.NEW_YEAR_CAKE,
        ItemNumberEntry.RANCHERS_BOOTS_SPEED,
        ItemNumberEntry.LARVA_HOOK,
        ItemNumberEntry.VACUUM_GARDEN,
    )

    enum class ItemNumberEntry(private val displayName: String) {
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

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Highlight Widgets", desc = "Highlight enabled and disabled widgets in /tab.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightWidgets: Boolean = true

    @Expose
    @ConfigOption(name = " Vacuum Bag Cap", desc = "Cap the Garden Vacuum Bag item number display to 40.")
    @ConfigEditorBoolean
    var vacuumBagCap: Boolean = true

    @Expose
    @ConfigOption(
        name = "Quick Craft Confirmation",
        desc = "Require Ctrl+Click to craft items that aren't often quick crafted " +
            "(e.g. armor, weapons, accessories). Sack items can be crafted normally.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var quickCraftingConfirmation: Boolean = false

    @Expose
    @ConfigOption(name = "Sack Name", desc = "Show an abbreviation of the sack name.")
    @ConfigEditorBoolean
    @FeatureToggle
    var displaySackName: Boolean = false

    @Expose
    @ConfigOption(
        name = "Anvil Combine Helper",
        desc = "Suggest the same item in the inventory when trying to combine two items in the anvil.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var anvilCombineHelper: Boolean = false

    @Expose
    @ConfigOption(name = "Item Stars", desc = "Show a compact star count in the item name for all items.")
    @ConfigEditorBoolean
    @FeatureToggle
    var itemStars: Boolean = false

    @Expose
    @ConfigOption(name = "Ultimate Enchant Star", desc = "Show a star on Enchanted Books with an Ultimate Enchant.")
    @ConfigEditorBoolean
    @FeatureToggle
    var ultimateEnchantStar: Boolean = false

    @Expose
    @ConfigOption(
        name = "Old SkyBlock Menu",
        desc = "Show old buttons in the SkyBlock Menu: Trade, Accessories, Potions, Quiver, Fishing and Sacks. " +
            "§cOnly works with the booster cookie effect active.",
    )
    @ConfigEditorBoolean
    @SearchTag("SB")
    @FeatureToggle
    var oldSkyBlockMenu: Boolean = false

    @Expose
    @ConfigOption(
        name = "Favorite Power Stone",
        desc = "Show your favorite power stones. You can add/remove them by shift clicking a Power Stone.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var favoritePowerStone: Boolean = false

    @Expose
    @ConfigOption(
        name = "Shift Click Equipment",
        desc = "Change normal clicks into shift clicks in equipment inventory.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var shiftClickForEquipment: Boolean = false

    @Expose
    @ConfigOption(
        name = "Shift Click NPC sell",
        desc = "Change normal clicks to shift clicks in npc inventory for selling.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var shiftClickNpcSell: Boolean = false

    @Expose
    @ConfigOption(
        name = "Shift Click Brewing",
        desc = "Change normal clicks to shift clicks in Brewing Stand inventory.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var shiftClickBrewing: Boolean = false

    @Expose
    @ConfigOption(
        name = "Stonk of Stonk Price",
        desc = "Show Price per Stonk when taking the minimum bid in Stonks Auction (Richard).",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var stonkOfStonkPrice: Boolean = true

    @Expose
    @ConfigOption(name = "Minister in Calendar", desc = "Show the Minister with their perk in the Calendar.")
    @ConfigEditorBoolean
    @FeatureToggle
    var ministerInCalendar: Boolean = true

    @Expose
    @ConfigOption(name = "Show hex as actual color", desc = "Changes the color of hex codes to the actual color.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hexAsColorInLore: Boolean = true

    @Expose
    @ConfigOption(
        name = "Essence Shop Helper",
        desc = "Show extra information about remaining upgrades in essence shops.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var essenceShopHelper: Boolean = true

    @Expose
    @ConfigOption(name = "Snake Game Keybinds", desc = "Use WASD-Keys to move around in the Abiphone snake game.")
    @ConfigEditorBoolean
    @FeatureToggle
    var snakeGameKeybinds: Boolean = true

    @Expose
    @ConfigOption(
        name = "Highlight Active Beacon Effect",
        desc = "Highlights the currently selected beacon effect in the beacon inventory.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightActiveBeaconEffect: Boolean = true

    @Expose
    @ConfigOption(
        name = "Save Private Island Chests",
        desc = "Saves every chest you looked at on your private island. The Data gets used by other features, " +
            "so this does not do anything directly noticeable",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var savePrivateIslandChests: Boolean = false
}
