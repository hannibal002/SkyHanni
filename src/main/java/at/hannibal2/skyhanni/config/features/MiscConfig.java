package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import io.github.moulberry.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiscConfig {

    @Expose
    @ConfigOption(name = "Pet", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean pet = false;

    @Expose
    @ConfigOption(name = "Pet Display", desc = "Show the currently active pet.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean petDisplay = false;

    @Expose
    @ConfigOption(name = "Pet Experience Tooltip", desc = "")
    @ConfigAccordionId(id = 0)
    @Accordion
    public PetExperienceToolTipConfig petExperienceToolTip = new PetExperienceToolTipConfig();

    public static class PetExperienceToolTipConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the full pet exp and the progress to level 100 (ignoring rarity) when hovering over an pet while pressing shift key.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean petDisplay = true;

        @Expose
        @ConfigOption(name = "Show Always", desc = "Show this info always, even if not pressing shift key.")
        @ConfigEditorBoolean
        public boolean showAlways = false;

        @Expose
        @ConfigOption(name = "GDrag 200", desc = "Show for Golden Dragon the exp needed for level 200.")
        @ConfigEditorBoolean
        public boolean goldenDragon200 = true;

    }

    @Expose
    public Position petDisplayPos = new Position(-330, -15, false, true);

    // rename this to just "time will cause a config reset
    @ConfigOption(name = "Time Features", desc = "")
    @Accordion
    @Expose
    public TimeConfig timeConfigs = new TimeConfig();

    public static class TimeConfig {

        @Expose
        @ConfigOption(name = "Real Time", desc = "Display the current computer time, a handy feature when playing in full-screen mode.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean realTime = false;

        @Expose
        public Position realTimePos = new Position(10, 10, false, true);

        @Expose
        @ConfigOption(name = "Winter Time", desc = "While on the Winter Island, show a timer until Jerry's Workshop closes.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean winterTime = true;

        @Expose
        public Position winterTimePos = new Position(10, 10, false, true);
    }

    @ConfigOption(name = "Hide Armor", desc = "")
    @Accordion
    @Expose
    public HideArmor hideArmor2 = new HideArmor();

    public static class HideArmor {

        @Expose
        @ConfigOption(name = "Mode", desc = "Hide the armor of players.")
        @ConfigEditorDropdown(values = {"All", "Own Armor", "Other's Armor", "Off"})
        @ConfigAccordionId(id = 1)
        public int mode = 3;

        @Expose
        @ConfigOption(name = "Only Helmet", desc = "Only hide the helmet.")
        @ConfigEditorBoolean()
        @ConfigAccordionId(id = 3)
        public Boolean onlyHelmet = false;

    }

    @Expose
    @ConfigOption(name = "Potion Effects", desc = "")
    @ConfigEditorAccordion(id = 5)
    public boolean potionEffects = false;

    @Expose
    @ConfigOption(name = "Non God Pot Effects", desc = "Display the active potion effects that are not part of the God Pot.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    @FeatureToggle
    public boolean nonGodPotEffectDisplay = false;

    @Expose
    @ConfigOption(name = "Show Mixins", desc = "Include god pot mixins in the Non God Pot Effects display.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    @FeatureToggle
    public boolean nonGodPotEffectShowMixins = false;

    @Expose
    public Position nonGodPotEffectPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Tia Relay", desc = "")
    @ConfigEditorAccordion(id = 7)
    public boolean tiaRelay = false;

    @Expose
    @ConfigOption(name = "Tia Relay Waypoint", desc = "Show the next relay waypoint for Tia the Fairy, where maintenance for the Abiphone network needs to be done.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    @FeatureToggle
    public boolean tiaRelayNextWaypoint = true;

    @Expose
    @ConfigOption(name = "Tia Relay All", desc = "Show all relay waypoints at once.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    public boolean tiaRelayAllWaypoints = false;

    @Expose
    @ConfigOption(name = "Tia Relay Helper", desc = "Helps with solving the sound puzzle.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    @FeatureToggle
    public boolean tiaRelayHelper = true;

    @Expose
    @ConfigOption(name = "Tia Relay Mute", desc = "Mutes the sound when close to the relay.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    @FeatureToggle
    public boolean tiaRelayMute = true;

    @Expose
    @ConfigOption(name = "Tps Display", desc = "")
    @ConfigEditorAccordion(id = 8)
    public boolean tpsDisplay = false;

    @Expose
    @ConfigOption(name = "Tps Display", desc = "Show the TPS of the current server, like in Soopy.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 8)
    @FeatureToggle
    public boolean tpsDisplayEnabled = false;

    @Expose
    public Position tpsDisplayPosition = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Particle Hider", desc = "")
    @ConfigEditorAccordion(id = 9)
    public boolean particleHider = false;

    @Expose
    @ConfigOption(name = "Blaze Particles", desc = "Hide Blaze particles.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    @FeatureToggle
    public boolean hideBlazeParticles = false;

    @Expose
    @ConfigOption(name = "Enderman Particles", desc = "Hide Enderman particles.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    @FeatureToggle
    public boolean hideEndermanParticles = false;

    @Expose
    @ConfigOption(name = "Fireball Particles", desc = "Hide fireball particles.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    @FeatureToggle
    public boolean hideFireballParticles = true;

    @Expose
    @ConfigOption(name = "Fire Particles", desc = "Hide particles from the fire block.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    @FeatureToggle
    public boolean hideFireBlockParticles = true;

    @Expose
    @ConfigOption(name = "Smoke Particles", desc = "Hide smoke particles.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    @FeatureToggle
    public boolean hideSmokeParticles = false;

    @Expose
    @ConfigOption(name = "Far Particles", desc = "Hide particles that are more than 40 blocks away.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    @FeatureToggle
    public boolean hideFarParticles = true;

    @Expose
    @ConfigOption(name = "Close Redstone Particles", desc = "Hide Redstone particles around the player (appear for some potion effects).")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    @FeatureToggle
    public boolean hideCloseRedstoneparticles = true;

    @Expose
    @ConfigOption(name = "Chicken Head Timer", desc = "")
    @ConfigEditorAccordion(id = 10)
    public boolean chickenHeadTimer = false;

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the cooldown until the next time you can lay an egg with the Chicken Head.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 10)
    @FeatureToggle
    public boolean chickenHeadTimerDisplay = false;

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the 'You laid an egg!' chat message.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 10)
    @FeatureToggle
    public boolean chickenHeadTimerHideChat = true;

    @Expose
    public Position chickenHeadTimerPosition = new Position(-372, 73, false, true);

    @Expose
    @ConfigOption(name = "Estimated Item Value", desc = "(Prices for Enchantments, Reforge Stones, Gemstones, Drill Parts and more)")
    @ConfigEditorAccordion(id = 11)
    public boolean estimatedItemValue = false;

    @Expose
    @ConfigOption(name = "Enable Estimated Price", desc = "Displays an Estimated Item Value for the item you hover over.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    @FeatureToggle
    public boolean estimatedIemValueEnabled = false;

    @Expose
    @ConfigOption(name = "Hotkey", desc = "Press this key to show the Estimated Item Value.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    @ConfigAccordionId(id = 11)
    public int estimatedItemValueHotkey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Show always", desc = "Ignore the hotkey and always display the item value.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean estimatedIemValueAlwaysEnabled = true;

    @Expose
    @ConfigOption(name = "Enchantments Cap", desc = "Only show the top # most expensive enchantments.")
    @ConfigEditorSlider(
            minValue = 1,
            maxValue = 30,
            minStep = 1
    )
    @ConfigAccordionId(id = 11)
    public Property<Integer> estimatedIemValueEnchantmentsCap = Property.of(7);


    @Expose
    @ConfigOption(name = "Show Exact Price", desc = "Show the exact total price instead of the compact number.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean estimatedIemValueExactPrice = false;

    @Expose
    @ConfigOption(name = "Show Armor Value", desc = "Show the value of the full armor set in the Wardrobe inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    @FeatureToggle
    public boolean estimatedIemValueArmor = true;

    @Expose
    public Position itemPriceDataPos = new Position(140, 90, false, true);

    @ConfigOption(name = "Discord Rich Presence", desc = "")
    @Accordion
    @Expose
    public DiscordRPC discordRPC = new DiscordRPC();

    public static class DiscordRPC {

        @Expose
        @ConfigOption(name = "Enable Discord RPC", desc = "Details about your SkyBlock session displayed through Discord.")
        @ConfigEditorBoolean
        @FeatureToggle
        public Property<Boolean> enabled = Property.of(false);

        @Expose
        @ConfigOption(name = "First Line", desc = "Decide what to show in the first line.")
        @ConfigEditorDropdown(values = {
                "Nothing",
                "Location",
                "Purse",
                "Bits",
                "Stats",
                "Held Item",
                "SkyBlock Date",
                "Profile",
                "Slayer",
                "Custom",
                "Dynamic",
                "Crop Milestone",
                "Current Pet"
        })
        public Property<Integer> firstLine = Property.of(0);

        @Expose
        @ConfigOption(name = "Second Line", desc = "Decide what to show in the second line.")
        @ConfigEditorDropdown(values = {
                "Nothing",
                "Location",
                "Purse",
                "Bits",
                "Stats",
                "Held Item",
                "SkyBlock Date",
                "Profile",
                "Slayer",
                "Custom",
                "Dynamic",
                "Crop Milestone",
                "Current Pet"
        })
        public Property<Integer> secondLine = Property.of(0);

        @Expose
        @ConfigOption(name = "Custom", desc = "What should be displayed if you select \"Custom\" above.")
        @ConfigEditorText
        public Property<String> customText = Property.of("");

        @Expose
        @ConfigOption(name = "Dynamic", desc = "\"Dynamic\" above shows your Crop Milestone, Slayer progress, or Stacking enchantment when possible, but this if you're doing none of them.")
        @ConfigEditorDropdown(values = {
                "Nothing",
                "Location",
                "Purse",
                "Bits",
                "Stats",
                "Held Item",
                "SkyBlock Date",
                "Profile",
                "Slayer",
                "Custom",
                "Crop Milestone",
                "Current Pet"
        })
        public Property<Integer> auto = Property.of(0);
    }

    @ConfigOption(name = "Trevor The Trapper", desc = "")
    @Accordion
    @Expose
    public TrevorTheTrapper trevorTheTrapper = new TrevorTheTrapper();

    public static class TrevorTheTrapper {

        @Expose
        @ConfigOption(name = "Trapper Solver", desc = "Assists you in finding Trevor's mobs. §eNote: May not always work as expected. " +
                "§cWill not help you to find rabbits or sheep in the Oasis!")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean trapperSolver = true;

        @Expose
        @ConfigOption(name = "Mob Dead Warning", desc = "Show a message when Trevor's mob dies.")
        @ConfigEditorBoolean
        public boolean trapperMobDiedMessage = true;

        @Expose
        @ConfigOption(name = "Warp to Trapper", desc = "Warp to Trevor's Den. Works only inside the Farming Islands.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean warpToTrapper = false;

        @Expose
        @ConfigOption(name = "Warp Hotkey", desc = "Press this key to warp to Trevor's Den.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int keyBindWarpTrapper = Keyboard.KEY_NONE;

        @Expose
        @ConfigOption(name = "Trapper Cooldown", desc = "Change the color of Trevor and adds a cooldown over his head.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean trapperTalkCooldown = true;

        @Expose
        @ConfigOption(
                name = "Trapper Cooldown GUI",
                desc = "Show the cooldown on screen in an overlay (intended for Abiphone users)."
        )
        @ConfigEditorBoolean
        public boolean trapperCooldownGui = false;

        @Expose
        public Position trapperCooldownPos = new Position(10, 10, false, true);
    }

    @ConfigOption(name = "Teleport Pads On Private Island", desc = "")
    @Accordion
    @Expose
    public TeleportPad teleportPad = new TeleportPad();

    public static class TeleportPad {

        @Expose
        @ConfigOption(name = "Compact Name", desc = "Hide the 'Warp to' and 'No Destination' texts over teleport pads.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean compactName = false;

        @Expose
        @ConfigOption(name = "Inventory Numbers", desc = "Show the number of the teleport pads inside the 'Change Destination' inventory as stack size.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean inventoryNumbers = false;
    }

    @ConfigOption(name = "City Project", desc = "")
    @Accordion
    @Expose
    public CityProject cityProject = new CityProject();

    public static class CityProject {

        @Expose
        @ConfigOption(name = "Show Materials", desc = "Show materials needed for contributing to the City Project.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean showMaterials = true;

        @Expose
        @ConfigOption(name = "Show Ready", desc = "Mark contributions that are ready to participate.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean showReady = true;

        @Expose
        @ConfigOption(name = "Daily Reminder", desc = "Remind every 24 hours to participate.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean dailyReminder = true;

        @Expose
        public Position pos = new Position(150, 150, false, true);
    }

    @ConfigOption(name = "Pocket Sack-In-A-Sack", desc = "")
    @Accordion
    @Expose
    public PocketSackInASack pocketSackInASack = new PocketSackInASack();

    public static class PocketSackInASack {

        @Expose
        @ConfigOption(name = "Show in Overlay", desc = "Show the number of Pocket Sack-In-A-Sack applied on a sack icon as an overlay.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean showOverlay = false;

        @Expose
        @ConfigOption(name = "Replace In Lore", desc = "Replace how text is displayed in lore.\nShow §eis stitched with 2/3...\n§7Instead of §eis stitched with two...")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean replaceLore = true;
    }

    @ConfigOption(name = "Quick Mod Menu Switch", desc = "")
    @Accordion
    @Expose
    public QuickModMenuSwitch quickModMenuSwitch = new QuickModMenuSwitch();

    public static class QuickModMenuSwitch {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Adding a mod list, allowing to quickly switch between different mod menus.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Inside Escape Menu", desc = "Show the mod list while inside the Escape menu.")
        @ConfigEditorBoolean
        public boolean insideEscapeMenu = true;

        @Expose
        @ConfigOption(name = "Inside Inventory", desc = "Show the mod list while inside the player inventory (no chest inventory).")
        @ConfigEditorBoolean
        public boolean insidePlayerInventory = false;

        @Expose
        public Position pos = new Position(-178, 143, false, true);
    }

    @Expose
    @ConfigOption(name = "Frozen Treasure Tracker", desc = "")
    @Accordion
    public MiscConfig.FrozenTreasureTracker frozenTreasureTracker = new MiscConfig.FrozenTreasureTracker();

    public static class FrozenTreasureTracker {

        @Expose
        @ConfigOption(
                name = "Enabled",
                desc = "Tracks all of your drops from Frozen Treasure in the Glacial Caves.\n" +
                        "§eIce calculations are an estimate but are relatively accurate."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(
                name = "Text Format",
                desc = "Drag text to change the appearance of the overlay."
        )
        @ConfigEditorDraggableList(
                exampleText = {
                        "§1§lFrozen Treasure Tracker",
                        "§61,636 Treasures Mined",
                        "§33.2m Total Ice",
                        "§3342,192 Ice/hr",
                        "§81,002 Compact Procs",
                        " ",
                        "§b182 §fWhite Gift",
                        "§b94 §aGreen Gift",
                        "§b17 §9§cRed Gift",
                        "§b328 §fPacked Ice",
                        "§b80 §aEnchanted Ice",
                        "§b4 §9Enchanted Packed Ice",
                        "§b182 §aIce Bait",
                        "§b3 §aGlowy Chum Bait",
                        "§b36 §5Glacial Fragment",
                        "§b6 §fGlacial Talisman",
                        " ",
                }
        )
        public List<Integer> textFormat = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 14, 15));

        @Expose
        @ConfigOption(name = "Only in Glacial Cave", desc = "Only shows the overlay while in the Glacial Cave.")
        @ConfigEditorBoolean
        public boolean onlyInCave = true;

        @Expose
        @ConfigOption(name = "Show as drops", desc = "Multiplies the numbers on the display by the base drop. \n" +
                "E.g. 3 Ice Bait -> 48 Ice Bait")
        @ConfigEditorBoolean
        public boolean showAsDrops = false;

        @Expose
        @ConfigOption(name = "Hide Chat messages", desc = "Hides the chat messages from Frozen Treasures.")
        @ConfigEditorBoolean
        public boolean hideMessages = false;

        @Expose
        public Position position = new Position(10, 80, false, true);
    }

    @Expose
    @ConfigOption(name = "Ender Node Tracker", desc = "")
    @Accordion
    public EnderNodeTracker enderNodeTracker = new EnderNodeTracker();

    public static class EnderNodeTracker {
        @Expose
        @ConfigOption(
                name = "Enabled",
                desc = "Tracks all of your drops from mining Ender Nodes in the End.\n" +
                        "Also tracks drops from Endermen."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(
                name = "Text Format",
                desc = "Drag text to change the appearance of the overlay."
        )
        @ConfigEditorDraggableList(
                exampleText = {
                        "§5§lEnder Node Tracker",
                        "§d1,303 Ender Nodes Mined",
                        "§615.3M Coins Made",
                        " ",
                        "§b123 §cEndermite Nest",
                        "§b832 §aEnchanted End Stone",
                        "§b230 §aEnchanted Obsidian",
                        "§b1630 §aEnchanted Ender Pearl",
                        "§b85 §aGrand Experience Bottle",
                        "§b4 §9Titanic Experience Bottle",
                        "§b15 §9End Stone Shulker",
                        "§b53 §9End Stone Geode",
                        "§b10 §d◆ Magical Rune I",
                        "§b24 §5Ender Gauntlet",
                        "§b357 §5Mite Gel",
                        "§b2 §cShrimp The Fish",
                        " ",
                        "§b200 §5Ender Armor",
                        "§b24 §5Ender Helmet",
                        "§b24 §5Ender Chestplate",
                        "§b24 §5Ender Leggings",
                        "§b24 §5Ender Boots",
                        "§b24 §5Ender Necklace",
                        "§f10§7-§a8§7-§93§7-§52§7-§61 §fEnderman Pet",
                        " "
                }
        )
        public Property<List<Integer>> textFormat = Property.of(new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 14, 15, 16, 17, 23)));

        @Expose
        public Position position = new Position(10, 80, false, true);
    }

    @Expose
    @ConfigOption(name = "Custom Text box", desc = "")
    @Accordion
    public TextBox textBox = new TextBox();

    public static class TextBox {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Enables showing the textbox while in SkyBlock.")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Text", desc = "Enter text you want to display here.\n" +
                "§eUse '&' as the colour code character.\n" +
                "§eUse '\\n' as the line break character.")
        @ConfigEditorText
        public Property<String> text = Property.of("&aYour Text Here\\n&bYour new line here");

        @Expose
        public Position position = new Position(10, 80, false, true);
    }

    @Expose
    @ConfigOption(name = "Bestiary Data", desc = "")
    @Accordion
    public BestiaryDataConfig bestiaryData = new BestiaryDataConfig();

    public static class BestiaryDataConfig {
        @Expose
        @ConfigOption(name = "Enable", desc = "Show Bestiary Data overlay in the Bestiary menu.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Number format", desc = "Short: 1.1k\nLong: 1.100")
        @ConfigEditorDropdown(values = {"Short", "Long"})
        public int numberFormat = 0;

        @Expose
        @ConfigOption(name = "Display type", desc = "Choose what the display should show")
        @ConfigEditorDropdown(values = {
                "Global to max",
                "Global to next tier",
                "Lowest total kills",
                "Highest total kills",
                "Lowest kills needed to max",
                "Highest kills needed to max",
                "Lowest kills needed to next tier",
                "Highest kills needed to next tier"
        })
        public int displayType = 0;

        @Expose
        @ConfigOption(name = "Hide maxed", desc = "Hide maxed mobs")
        @ConfigEditorBoolean
        public boolean hideMaxed = false;

        @Expose
        @ConfigOption(name = "Replace Romans", desc = "Replace Roman numerals (IX) with regular numbers (9)")
        @ConfigEditorBoolean
        public boolean replaceRoman = false;

        @Expose
        public Position position = new Position(100, 100, false, true);
    }

    @Expose
    @ConfigOption(name = "Cosmetic", desc = "")
    @Accordion
    public CosmeticConfig cosmeticConfig = new CosmeticConfig();

    public static class CosmeticConfig {

        @Expose
        @ConfigOption(name = "Following Line", desc = "")
        @Accordion
        public FollowingLineConfig followingLineConfig = new FollowingLineConfig();

        public static class FollowingLineConfig {

            @Expose
            @ConfigOption(name = "Enabled", desc = "Draw a colored line behind the player.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = false;

            @Expose
            @ConfigOption(name = "Line color", desc = "Color of the line.")
            @ConfigEditorColour
            public String lineColor = "0:255:255:255:255";

            @Expose
            @ConfigOption(name = "Time Alive", desc = "Time in seconds until the line fades out.")
            @ConfigEditorSlider(minStep = 1, minValue = 1, maxValue = 30)
            public int secondsAlive = 3;

            @Expose
            @ConfigOption(name = "Max Line Width", desc = "Max width of the line.")
            @ConfigEditorSlider(minStep = 1, minValue = 1, maxValue = 10)
            public int lineWidth = 4;

            @Expose
            @ConfigOption(name = "Behind Blocks", desc = "Show behind blocks.")
            @ConfigEditorBoolean
            public boolean behindBlocks = false;
        }
    }


    @Expose
    @ConfigOption(name = "Glowing Dropped Items", desc = "")
    @Accordion
    public GlowingDroppedItems glowingDroppedItems = new GlowingDroppedItems();

    public static class GlowingDroppedItems {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Draws a glowing outline around all dropped items on the ground.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Highlight Showcase Items", desc = "Draws a glowing outline around showcase items.")
        @ConfigEditorBoolean
        public boolean highlightShowcase = false;

        @Expose
        @ConfigOption(name = "Highlight Fishing Bait", desc = "Draws a glowing outline around fishing bait.")
        @ConfigEditorBoolean
        public boolean highlightFishingBait = false;

    }

    @Expose
    @ConfigOption(name = "Exp Bottles", desc = "Hides all the experience orbs lying on the ground.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideExpBottles = false;

    @Expose
    public Position collectionCounterPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Brewing Stand Overlay", desc = "Display the Item names directly inside the Brewing Stand.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean brewingStandOverlay = true;

    @Expose
    @ConfigOption(name = "Red Scoreboard Numbers", desc = "Hide the red scoreboard numbers on the right side of the screen.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideScoreboardNumbers = false;

    @Expose
    @ConfigOption(name = "Hide Piggy", desc = "Replacing 'Piggy' with 'Purse' in the Scoreboard.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hidePiggyScoreboard = true;

    @Expose
    @ConfigOption(name = "Explosions Hider", desc = "Hide explosions.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideExplosions = false;

    @Expose
    @ConfigOption(name = "CH Join", desc = "Helps buy a Pass for accessing the Crystal Hollows if needed.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean crystalHollowsJoin = true;

    @Expose
    @ConfigOption(name = "Fire Overlay Hider", desc = "Hide the fire overlay (Like in Skytils).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideFireOverlay = false;

    @Expose
    @ConfigOption(name = "Paste Into Signs", desc = "Allows you to paste the clipboard into signs when you press Ctrl + V.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean pasteIntoSigns = true;

    @Expose
    @ConfigOption(name = "Movement Speed", desc = "Show the player movement speed in blocks per second.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean playerMovementSpeed = false;

    @Expose
    public Position playerMovementSpeedPos = new Position(394, 124, false, true);

    @Expose
    @ConfigOption(name = "Pet Candy Used", desc = "Show the number of Pet Candy used on a pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean petCandyUsed = true;

    @Expose
    @ConfigOption(name = "Server Restart Title", desc = "Show a title with seconds remaining until the server restarts after a Game Update or Scheduled Restart.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean serverRestartTitle = true;

    @Expose
    @ConfigOption(name = "Piece Of Wizard Portal", desc = "Restore the Earned By lore line on bought Piece Of Wizard Portal.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean restorePieceOfWizardPortalLore = true;

    @Expose
    @ConfigOption(name = "Patcher Coords Waypoint", desc = "Highlight the coordinates sent by Patcher.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean patcherSendCoordWaypoint = false;

    @Expose
    @ConfigOption(name = "Harp Keybinds", desc = "In Melody's Harp, press buttons with your number row on the keyboard instead of clicking.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean harpKeybinds = false;

    @Expose
    @ConfigOption(name = "Harp Numbers", desc = "In Melody's Harp, show buttons as stack size (intended to be used with Harp Keybinds).")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean harpNumbers = false;

    @Expose
    @ConfigOption(name = "Account Upgrade Reminder", desc = "Remind you to claim account upgrades when complete.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean accountUpgradeReminder = true;

    @Expose
    @ConfigOption(name = "Superpairs Clicks Alert", desc = "Display an alert when you reach the maximum clicks gained from Chronomatron or Ultrasequencer.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean superpairsClicksAlert = false;

    @Expose
    @ConfigOption(name = "Config Button", desc = "Add a button to the pause menu to configure SkyHanni.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean configButtonOnPause = true;

    @Expose
    public Position inventoryLoadPos = new Position(394, 124, false, true);


}
