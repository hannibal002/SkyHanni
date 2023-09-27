package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigAccordionId;
import io.github.moulberry.moulconfig.annotations.ConfigEditorAccordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
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
    public boolean petDisplay = false;

    @Expose
    public Position petDisplayPos = new Position(-330, -15, false, true);

    @Expose
    @ConfigOption(name = "Time", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean time = false;

    @Expose
    @ConfigOption(name = "Real Time", desc = "Display the current computer time, a handy feature when playing in full-screen mode.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean realTime = false;

    @Expose
    @ConfigOption(name = "Real Time 12h Format", desc = "Display the current computer time in 12hr Format rather than 24h Format.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean realTimeFormatToggle = false;

    @Expose
    public Position realTimePos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Time on SkyBlock", desc = "Show the time on SkyBLock for this session.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean timeOnSkyBlockSession = false;

    @Expose
    public Position timeOnSkyBlockSessionPos = new Position(30, 10, false, true);

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
    @ConfigOption(name = "Damage Splash", desc = "")
    @ConfigEditorAccordion(id = 4)
    public boolean damageSplash = false;

    @Expose
    @ConfigOption(name = "Hide Damage Splash", desc = "Hide all damage splashes anywhere in SkyBlock.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean hideDamageSplash = false;

    @Expose
    @ConfigOption(name = "Potion Effects", desc = "")
    @ConfigEditorAccordion(id = 5)
    public boolean potionEffects = false;

    @Expose
    @ConfigOption(name = "Non God Pot Effects", desc = "Display the active potion effects that are not part of the god pot.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    public boolean nonGodPotEffectDisplay = false;

    @Expose
    @ConfigOption(name = "Show Mixins", desc = "Include god pot mixins in the non god pot effects display.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    public boolean nonGodPotEffectShowMixins = false;

    @Expose
    public Position nonGodPotEffectPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Crimson Reputation Helper", desc = "")
    @ConfigEditorAccordion(id = 6)
    public boolean reputationHelper = false;

    @Expose
    @ConfigOption(name = "Crimson Isle Reputation", desc = "Enable features around Reputation features in the Crimson Isle.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean crimsonIsleReputationHelper = true;

    @Expose
    @ConfigOption(name = "Use Hotkey", desc = "Only show the reputation helper while pressing the hotkey.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean reputationHelperUseHotkey = false;

    @Expose
    @ConfigOption(name = "Hotkey", desc = "Press this hotkey to show the reputation helper.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    @ConfigAccordionId(id = 6)
    public int reputationHelperHotkey = Keyboard.KEY_NONE;


    @Expose
    public Position crimsonIsleReputationHelperPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Reputation Locations", desc = "Crimson Isles waypoints for locations to get reputation.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean crimsonIsleReputationLocation = false;

    @Expose
    @ConfigOption(name = "Tia Relay", desc = "")
    @ConfigEditorAccordion(id = 7)
    public boolean tiaRelay = false;

    @Expose
    @ConfigOption(name = "Tia Relay Waypoint", desc = "Show the next relay waypoint for Tia the Fairy, where maintenance for the abiphone network needs to be done.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
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
    public boolean tiaRelayHelper = true;

    @Expose
    @ConfigOption(name = "Tia Relay Mute", desc = "Mutes the sound when close to the relay.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    public boolean tiaRelayMute = true;

    @Expose
    @ConfigOption(name = "Tps Display", desc = "")
    @ConfigEditorAccordion(id = 8)
    public boolean tpsDisplay = false;

    @Expose
    @ConfigOption(name = "Tps Display", desc = "Show the TPS of the current server, like in Soopy.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 8)
    public boolean tpsDisplayEnabled = false;

    @Expose
    public Position tpsDisplayPosition = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Particle Hider", desc = "")
    @ConfigEditorAccordion(id = 9)
    public boolean particleHider = false;

    @Expose
    @ConfigOption(name = "Blaze Particles", desc = "Hide blaze particles.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean hideBlazeParticles = false;

    @Expose
    @ConfigOption(name = "Enderman Particles", desc = "Hide enderman particles.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean hideEndermanParticles = false;

    @Expose
    @ConfigOption(name = "Fireball Particles", desc = "Hide fireball particles.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean hideFireballParticles = true;

    @Expose
    @ConfigOption(name = "Fire Particles", desc = "Hide particles from the fire block.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean hideFireBlockParticles = true;

    @Expose
    @ConfigOption(name = "Smoke Particles", desc = "Hide smoke particles.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean hideSmokeParticles = false;

    @Expose
    @ConfigOption(name = "Far Particles", desc = "Hide particles that are more than 40 blocks away.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean hideFarParticles = true;

    @Expose
    @ConfigOption(name = "Close Redstone Particles", desc = "Hide redstone particles around the player (appear for some potion effects).")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean hideCloseRedstoneparticles = true;

    @Expose
    @ConfigOption(name = "Chicken Head Timer", desc = "")
    @ConfigEditorAccordion(id = 10)
    public boolean chickenHeadTimer = false;

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the cooldown until the next time you can lay an egg with the Chicken Head.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 10)
    public boolean chickenHeadTimerDisplay = false;

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the 'You laid an egg!' chat message.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 10)
    public boolean chickenHeadTimerHideChat = true;

    @Expose
    public Position chickenHeadTimerPosition = new Position(-372, 73, false, true);

    @Expose
    @ConfigOption(name = "Estimated Item Value", desc = "(Enchantments, reforging stone prices, gemstones, gemstones, drill parts and more)")
    @ConfigEditorAccordion(id = 11)
    public boolean estimatedItemValue = false;

    @Expose
    @ConfigOption(name = "Enable Estimated Price", desc = "Displays an estimated item value for the item you hover over.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean estimatedIemValueEnabled = false;

    @Expose
    @ConfigOption(name = "Hotkey", desc = "Press this key to show the estimated item value.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    @ConfigAccordionId(id = 11)
    public int estimatedItemValueHotkey = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Show always", desc = "Ignore the hotkey and always display the item value.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean estimatedIemValueAlwaysEnabled = true;

    @Expose
    @ConfigOption(name = "Show Exact Price", desc = "Show the exact total price instead of the compact number.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean estimatedIemValueExactPrice = false;

    @Expose
    @ConfigOption(name = "Show Armor Value", desc = "Show the value of the full armor in the wardrobe inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
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
                "Skyblock Date",
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
                "Skyblock Date",
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
                "Skyblock Date",
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
        public boolean trapperSolver = true;

        @Expose
        @ConfigOption(name = "Mob Dead Warning", desc = "Show a message when Trevor's mob dies.")
        @ConfigEditorBoolean
        public boolean trapperMobDiedMessage = true;

        @Expose
        @ConfigOption(name = "Warp to Trapper", desc = "Warp to Trevor's Den. Works only inside the Farming Islands.")
        @ConfigEditorBoolean
        public boolean warpToTrapper = false;

        @Expose
        @ConfigOption(name = "Warp Hotkey", desc = "Press this key to warp to Trevor's Den.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int keyBindWarpTrapper = Keyboard.KEY_NONE;

        @Expose
        @ConfigOption(name = "Trapper Cooldown", desc = "Change the color of Trevor and adds a cooldown over his head.")
        @ConfigEditorBoolean
        public boolean trapperTalkCooldown = true;
    }

    @ConfigOption(name = "Teleport Pads On Private Island", desc = "")
    @Accordion
    @Expose
    public TeleportPad teleportPad = new TeleportPad();

    public static class TeleportPad {

        @Expose
        @ConfigOption(name = "Compact Name", desc = "Hide the 'Warp to' and 'No Destination' texts over teleport pads.")
        @ConfigEditorBoolean
        public boolean compactName = false;

        @Expose
        @ConfigOption(name = "Inventory Numbers", desc = "Show the number of the teleport pads inside the 'Change Destination' inventory as stack size.")
        @ConfigEditorBoolean
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
        public boolean showMaterials = true;

        @Expose
        @ConfigOption(name = "Show Ready", desc = "Mark contributions that are ready to participate.")
        @ConfigEditorBoolean
        public boolean showReady = true;

        @Expose
        @ConfigOption(name = "Daily Reminder", desc = "Remind every 24 hours to participate.")
        @ConfigEditorBoolean
        public boolean dailyReminder = true;

        @Expose
        public Position pos = new Position(150, 150, false, true);
    }

    @ConfigOption(name = "Tab Complete Commands", desc = "")
    @Accordion
    @Expose
    public TabCompleteCommands tabCompleteCommands = new TabCompleteCommands();

    public static class TabCompleteCommands {

        @Expose
        @ConfigOption(name = "Warps", desc = "Tab complete the warp-point names when typing §e/warp <TAB>§7.")
        @ConfigEditorBoolean
        public boolean warps = true;

        @Expose
        @ConfigOption(name = "Island Players", desc = "Tab complete other players on the same island.")
        @ConfigEditorBoolean
        public boolean islandPlayers = true;

        @Expose
        @ConfigOption(name = "Friends", desc = "Tab complete friends from your friends list.")
        @ConfigEditorBoolean
        public boolean friends = true;

        @Expose
        @ConfigOption(name = "Only Best Friends", desc = "Only Tab Complete best friends.")
        @ConfigEditorBoolean
        public boolean onlyBestFriends = false;

        @Expose
        @ConfigOption(name = "Party", desc = "Tab complete party members.")
        @ConfigEditorBoolean
        public boolean party = true;

        @Expose
        @ConfigOption(name = "VIP Visits", desc = "Tab complete the visit to special users with cake souls on it.")
        @ConfigEditorBoolean
        public boolean vipVisits = true;
    }

    @ConfigOption(name = "Pocket Sack-In-A-Sack", desc = "")
    @Accordion
    @Expose
    public PocketSackInASack pocketSackInASack = new PocketSackInASack();

    public static class PocketSackInASack {

        @Expose
        @ConfigOption(name = "Show in Overlay", desc = "Show the number of Pocket Sack-In-A-Sack applied on a sack icon as an overlay.")
        @ConfigEditorBoolean
        public boolean showOverlay = false;

        @Expose
        @ConfigOption(name = "Replace In Lore", desc = "Replace how text is displayed in lore.\nShow §eis stitched with 2/3...\n§7Instead of §eis stitched with two...")
        @ConfigEditorBoolean
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
                desc = "Tracks all of your drops from frozen treasure in the Glacial Caves.\n" +
                        "§eIce calculations are an estimate but are relatively accurate."
        )
        @ConfigEditorBoolean
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
    public EnderNodeTrackerConfig enderNodeTracker = new EnderNodeTrackerConfig();

    public static class EnderNodeTrackerConfig {
        @Expose
        @ConfigOption(
                name = "Enabled",
                desc = "Tracks all of your drops from mining Ender Nodes in the End.\n" +
                        "Also tracks drops from Endermen."
        )
        @ConfigEditorBoolean
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
        @ConfigOption(name = "Enable", desc = "Show bestiary data overlay in the bestiary menu.")
        @ConfigEditorBoolean
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
        @ConfigOption(name = "Replace romans", desc = "Replace romans numeral (IX) with regular number (9)")
        @ConfigEditorBoolean
        public boolean replaceRoman = false;

        @Expose
        public Position position = new Position(100, 100, false, true);
    }

    @Expose
    @ConfigOption(name = "Mining", desc = "")
    @Accordion
    public MiningConfig mining = new MiningConfig();

    public static class MiningConfig {

        @Expose
        @ConfigOption(name = "Highlight Commission Mobs", desc = "Highlight Mobs that are part of active commissions.")
        @ConfigEditorBoolean
        public boolean highlightCommissionMobs = false;

        @Expose
        @ConfigOption(name = "King Talisman Helper", desc = "Show kings you have not talked to yet, and when the next missing king will appear.")
        @ConfigEditorBoolean
        public boolean kingTalismanHelper = false;

        @Expose
        public Position kingTalismanHelperPos = new Position(-400, 220, false, true);

        @Expose
        @ConfigOption(name = "Names in Core", desc = "Show the names of the 4 areas while in the center of crystal hollows.")
        @ConfigEditorBoolean
        public boolean crystalHollowsNamesInCore = false;
    }

    @Expose
    @ConfigOption(name = "Exp Bottles", desc = "Hides all the experience orbs lying on the ground.")
    @ConfigEditorBoolean
    public boolean hideExpBottles = false;

    @Expose
    public Position collectionCounterPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Brewing Stand Overlay", desc = "Display the Item names directly inside the Brewing Stand.")
    @ConfigEditorBoolean
    public boolean brewingStandOverlay = true;

    @Expose
    @ConfigOption(name = "Red Scoreboard Numbers", desc = "Hide the red scoreboard numbers at the right side of the screen.")
    @ConfigEditorBoolean
    public boolean hideScoreboardNumbers = false;

    @Expose
    @ConfigOption(name = "Hide Piggy", desc = "Replacing 'Piggy' with 'Purse' in the Scoreboard.")
    @ConfigEditorBoolean
    public boolean hidePiggyScoreboard = true;

    @Expose
    @ConfigOption(name = "Explosions Hider", desc = "Hide explosions.")
    @ConfigEditorBoolean
    public boolean hideExplosions = false;

    @Expose
    @ConfigOption(name = "CH Join", desc = "Helps buy a Pass for accessing the Crystal Hollows if needed.")
    @ConfigEditorBoolean
    public boolean crystalHollowsJoin = true;

    @Expose
    @ConfigOption(name = "Fire Overlay Hider", desc = "Hide the fire overlay (Like in Skytils).")
    @ConfigEditorBoolean
    public boolean hideFireOverlay = false;

    @Expose
    @ConfigOption(name = "Paste Into Signs", desc = "Allows you to paste the clipboard into signs when you press Ctrl + V.")
    @ConfigEditorBoolean
    public boolean pasteIntoSigns = true;

    @Expose
    @ConfigOption(name = "Movement Speed", desc = "Show the player movement speed in blocks per second.")
    @ConfigEditorBoolean
    public boolean playerMovementSpeed = false;

    @Expose
    public Position playerMovementSpeedPos = new Position(394, 124, false, true);

    @Expose
    @ConfigOption(name = "Pet Candy Used", desc = "Show the number of pet candies used on a pet.")
    @ConfigEditorBoolean
    public boolean petCandyUsed = true;

    @Expose
    @ConfigOption(name = "Server Restart Title", desc = "Show a title with seconds remaining until the server restarts after a Game Update or Scheduled Restart.")
    @ConfigEditorBoolean
    public boolean serverRestartTitle = true;

    @Expose
    @ConfigOption(name = "Piece Of Wizard Portal", desc = "Restore the Earned By lore line on bought Piece Of Wizard Portal.")
    @ConfigEditorBoolean
    public boolean restorePieceOfWizardPortalLore = true;

    @Expose
    @ConfigOption(name = "Patcher Coords Waypoint", desc = "Highlight the coordinates sent by Patcher.")
    @ConfigEditorBoolean
    public boolean patcherSendCoordWaypoint = false;

    @Expose
    @ConfigOption(name = "Harp Keybinds", desc = "In Melodys Harp, press buttons with your number row on the keyboard instead of clicking.")
    @ConfigEditorBoolean
    public boolean harpKeybinds = false;

    @Expose
    @ConfigOption(name = "Config Button", desc = "Add a button to the pause menu to configure SkyHanni.")
    @ConfigEditorBoolean
    public boolean configButtonOnPause = true;

    @Expose
    public Position inventoryLoadPos = new Position(394, 124, false, true);
}
