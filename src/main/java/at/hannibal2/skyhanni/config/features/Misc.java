package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.misc.GhostCounter;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import io.github.moulberry.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Misc {

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
    public Position petDisplayPos = new Position(-111, 221, false, true);

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
    public Position realTimePos = new Position(10, 10, false, true);

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
    @ConfigOption(name = "Enabled", desc = "Show the cooldown until the next time you can lay an egg with the chicken head.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 10)
    public boolean chickenHeadTimerDisplay = false;

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the 'You lay an egg' chat message.")
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
                "Profile (Fruit)",
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
                "Profile (Fruit)",
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
        @ConfigOption(name = "Dynamic", desc = "\"Dynamic\" above shows your Crop Milestone or Slayer progress while doing those, but this if you're doing neither.")
        @ConfigEditorDropdown(values = {
                "Nothing",
                "Location",
                "Purse",
                "Bits",
                "Stats",
                "Held Item",
                "Skyblock Date",
                "Profile (Fruit)",
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
        @ConfigOption(name = "Show Ready", desc = "Mark Contributions that are ready to participate.")
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

    @ConfigOption(name = "Ghost Counter", desc = "")
    @Accordion
    @Expose
    public GhostCounterStorage ghostCounter = new GhostCounterStorage();

    public static class GhostCounterStorage {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Enable ghost counter.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(
                name = "Display Text",
                desc = "Drag text to change the appearance of the overlay."
        )
        @ConfigEditorDraggableList(
                exampleText = {
                        "§6Ghosts Counter",
                        "  §bGhost Killed: 42",
                        "  §bSorrow: 6",
                        "  §bGhost since Sorrow: 1",
                        "  §bGhosts/Sorrow: 5",
                        "  §bVolta: 6",
                        "  §bPlasma: 8",
                        "  §bGhostly Boots: 1",
                        "  §bBag Of Cash: 4",
                        "  §bAvg Magic Find: 271",
                        "  §bScavenger Coins: 15,000",
                        "  §bKill Combo: 14",
                        "  §bHighest Kill Combo: 96",
                        "  §bSkill XP Gained: 145,648",
                        "  §bBestiary 1: 0/10",
                        "  §bXP/h: 810,410"
                }
        )
        public List<Integer> ghostDisplayText = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 9, 10, 11, 12));

        @ConfigOption(name = "Text Formatting", desc = "")
        @Accordion
        @Expose
        public TextFormatting textFormatting = new TextFormatting();

        public static class TextFormatting {

            @ConfigOption(name = "§eText Formatting Info", desc = "§e%session% §ris §e§lalways §rreplaced with\n" +
                    "§7the count for your current session.\n" +
                    "§7Reset when restarting the game.\n" +
                    "§7You can use §e&Z §7color code to use SBA chroma")
            @ConfigEditorInfoText
            public boolean formatInfo = false;

            @ConfigOption(name = "Reset Formatting", desc = "Reset formatting to default text.")
            @ConfigEditorButton(buttonText = "Reset")
            public Runnable resetFormatting = GhostCounter.INSTANCE::resetFormatting;

            @Expose
            @ConfigOption(name = "Title", desc = "Title Line.")
            @ConfigEditorText
            public String titleFormat = "&6Ghost Counter";

            @Expose
            @ConfigOption(name = "Ghost Killed", desc = "Ghost Killed line.\n§e%value% §ris replaced with\n" +
                    "Ghost Killed.\n" +
                    "§r%session% is replaced with Ghost killed")
            @ConfigEditorText
            public String ghostKiledFormat = "  &6Ghost Killed: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Sorrows", desc = "Sorrows drop line.\n§e%value% §ris replaced with\nsorrows dropped.")
            @ConfigEditorText
            public String sorrowsFormat = "  &6Sorrow: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Ghost Since Sorrow", desc = "Ghost Since Sorrow line.\n§e%value% §ris replaced with\nGhost since last sorrow drop.")
            @ConfigEditorText
            public String ghostSinceSorrowFormat = "  &6Ghost since Sorrow: &b%value%";

            @Expose
            @ConfigOption(name = "Ghost Kill Per Sorrow", desc = "Ghost Kill Per Sorrow line.\n§e%value% §ris replaced with\naverage ghost kill per sorrow drop.")
            @ConfigEditorText
            public String ghostKillPerSorrowFormat = "  &6Ghosts/Sorrow: &b%value%";

            @Expose
            @ConfigOption(name = "Voltas", desc = "Voltas drop line.\n§e%value% §ris replaced with\nvoltas dropped.")
            @ConfigEditorText
            public String voltasFormat = "  &6Voltas: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Plasmas", desc = "Plasmas drop line.\n§e%value% §ris replaced with\nplasmas dropped.")
            @ConfigEditorText
            public String plasmasFormat = "  &6Plasmas: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Ghostly Boots", desc = "Ghostly Boots drop line.\n§e%value% §ris replaced with\nGhostly Boots dropped.")
            @ConfigEditorText
            public String ghostlyBootsFormat = "  &6Ghostly Boots: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Bag Of Cash", desc = "Bag Of Cash drop line.\n§e%value% §ris replaced with\nBag Of Cash dropped.")
            @ConfigEditorText
            public String bagOfCashFormat = "  &6Bag Of Cash: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Average Magic Find", desc = "Average Magic Find line.\n§e%value% §ris replaced with\nAverage Magic Find.")
            @ConfigEditorText
            public String avgMagicFindFormat = "  &6Avg Magic Find: &b%value%";

            @Expose
            @ConfigOption(name = "Scavenger Coins", desc = "Scavenger Coins line.\n§e%value% §ris replaced with\nCoins earned from kill ghosts.\nInclude: Scavenger Enchant, Scavenger Talismans, Kill Combo.")
            @ConfigEditorText
            public String scavengerCoinsFormat = "  &6Scavenger Coins: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Kill Combo", desc = "Kill Combo line.\n§e%value% §ris replaced with\nYour current kill combo.")
            @ConfigEditorText
            public String killComboFormat = "  &6Kill Combo: &b%value%";

            @Expose
            @ConfigOption(name = "Highest Kill Combo", desc = "Highest Kill Combo line.\n§e%value% §ris replaced with\nYour current highest kill combo.")
            @ConfigEditorText
            public String highestKillComboFormat = "  &6Highest Kill Combo: &b%value% &7(%session%)";

            @Expose
            @ConfigOption(name = "Skill XP Gained", desc = "Skill XP Gained line.\n§e%value% §ris replaced with\nSkill XP Gained from killing Ghosts.")
            @ConfigEditorText
            public String skillXPGainFormat = "  &6Skill XP Gained: &b%value% &7(%session%)";

            @ConfigOption(name = "Bestiary Formatting", desc = "")
            @Accordion
            @Expose
            public BestiaryFormatting bestiaryFormatting = new BestiaryFormatting();

            public static class BestiaryFormatting {

                @Expose
                @ConfigOption(name = "Bestiary", desc = "Bestiary Progress line.\n§e%value% §ris replaced with\n" +
                        "Your current progress to next level.\n" +
                        "%currentLevel% &4is replaced with your current bestiary level\n" +
                        "%nextLevel% &4is replaced with your current bestiary level +1.")
                @ConfigEditorText
                public String base = "  &6Bestiary %currentLevel%->%nextLevel%: &b%value%";

                @Expose
                @ConfigOption(name = "Nothing", desc = "Text to show when you need to open the\nBestiary Menu to gather data.")
                @ConfigEditorText
                public String openMenu = "§cOpen Bestiary Menu !";

                @Expose
                @ConfigOption(name = "Maxed", desc = "Text to show when your bestiary for ghost is at max level.")
                @ConfigEditorText
                public String maxed = "%currentKill% (&c&lMaxed!)";

                @Expose
                @ConfigOption(name = "Progress to Max", desc = "Text to show progress when the §eMaxed Bestiary§7 option is §aON")
                @ConfigEditorText
                public String showMax_progress = "%currentKill%/3M (%percentNumber%%)";

                @Expose
                @ConfigOption(name = "Progress", desc = "Text to show progress when the §eMaxed Bestiary§7 option is §cOFF")
                @ConfigEditorText
                public String progress = "%currentKill%/%killNeeded%";
            }

            @Expose
            @ConfigOption(name = "XP/h", desc = "XP Per Hour line.\n§e%value% §ris replaced with\nEstimated amount of combat xp you gain per hour.")
            @ConfigEditorText
            public String xpHourFormat = "  &6XP/h: &b%value%";
        }

        @Expose
        @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
        @ConfigEditorSlider(
                minValue = -5,
                maxValue = 10,
                minStep = 1)
        public int extraSpace = 1;

        @Expose
        @ConfigOption(name = "Show only in The Mist", desc = "Show the overlay only when you are in The Mist.")
        @ConfigEditorBoolean
        public boolean onlyOnMist = false;

        @Expose
        @ConfigOption(name = "Maxed Bestiary", desc = "Show progress to max bestiary instead of next level.")
        @ConfigEditorBoolean
        public boolean showMax = false;

        @ConfigOption(name = "Reset", desc = "Reset the counter.")
        @ConfigEditorButton(buttonText = "Reset")
        public Runnable resetCounter = GhostCounter.INSTANCE::reset;

        @Expose
        public Position position = new Position(50, 50, false, true);
    }

    @ConfigOption(name = "Pocket Sack-In-A-Sack", desc = "")
    @Accordion
    @Expose
    public PocketSackInASack pocketSackInASack = new PocketSackInASack();

    public static class PocketSackInASack {

        @Expose
        @ConfigOption(name = "Show in Overlay", desc = "Show numbers of Pocket Sack-In-A-Sack applied on a sack icon as overlay.")
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
        @ConfigOption(name = "Enabled", desc = "Adding a mod list, allowing to quickly switch between different mod menus")
        @ConfigEditorBoolean
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Inside Escape Menu", desc = "Show the mod list while inside the Escape menu")
        @ConfigEditorBoolean
        public boolean insideEscapeMenu = true;

        @Expose
        @ConfigOption(name = "Inside Inventory", desc = "Show the mod list while inside the player inventory (no chest inventory)")
        @ConfigEditorBoolean
        public boolean insidePlayerInventory = false;

        @Expose
        public Position pos = new Position(-178, 143, false, true);
    }

    @Expose
    @ConfigOption(name = "Exp Bottles", desc = "Hides all the experience orbs lying on the ground.")
    @ConfigEditorBoolean
    public boolean hideExpBottles = false;

    @Expose
    public Position collectionCounterPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Brewing Stand Overlay", desc = "Display the Item names directly inside the Brewing Stand")
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
    @ConfigOption(name = "Fire Overlay Hider", desc = "Hide the fire overlay (Like in Skytils)")
    @ConfigEditorBoolean
    public boolean hideFireOverlay = false;

    @Expose
    @ConfigOption(name = "Paste Into Signs", desc = "Allows you to paste the clipboard into signs when you press Ctrl + V")
    @ConfigEditorBoolean
    public boolean pasteIntoSigns = true;

    @Expose
    @ConfigOption(name = "Movement Speed", desc = "Show the player movement speed in blocks per second.")
    @ConfigEditorBoolean
    public boolean playerMovementSpeed = false;

    @Expose
    public Position playerMovementSpeedPos = new Position(394, 124, false, true);

    @Expose
    @ConfigOption(name = "Pet Candy Used", desc = "Show numbers of pet candies used on a pet.")
    @ConfigEditorBoolean
    public boolean petCandyUsed = true;

    @Expose
    @ConfigOption(name = "Server Restart Title", desc = "Show an title with seconds remaining until the server restarts after a Game Update or Scheduled Restart.")
    @ConfigEditorBoolean
    public boolean serverRestartTitle = true;

    @Expose
    @ConfigOption(name = "Piece Of Wizard Portal", desc = "Restore the Earned By lore line on bought Piece Of Wizard Portal.")
    @ConfigEditorBoolean
    public boolean restorePieceOfWizardPortalLore = true;

    @Expose
    @ConfigOption(name = "Config Button", desc = "Add a button to the pause menu to configure SkyHanni.")
    @ConfigEditorBoolean
    public boolean configButtonOnPause = true;

    @Expose
    public Position inventoryLoadPos = new Position(394, 124, false, true);
}
