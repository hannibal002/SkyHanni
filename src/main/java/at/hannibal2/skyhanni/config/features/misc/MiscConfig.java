package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.misc.compacttablist.CompactTabListConfig;
import at.hannibal2.skyhanni.config.features.misc.pets.PetConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiscConfig {

    @Expose
    @ConfigOption(name = "Pet", desc = "")
    @Accordion
    public PetConfig pets = new PetConfig();

    @ConfigOption(name = "Hide Armor", desc = "")
    @Accordion
    @Expose
    public HideArmorConfig hideArmor2 = new HideArmorConfig();

    @Expose
    @ConfigOption(name = "Potion Effects", desc = "")
    @Accordion
    public PotionEffectsConfig potionEffect = new PotionEffectsConfig();

    @Expose
    @ConfigOption(name = "Particle Hider", desc = "")
    @Accordion
    public ParticleHiderConfig particleHiders = new ParticleHiderConfig();

    @Expose
    @ConfigOption(name = "Estimated Item Value", desc = "(Prices for Enchantments, Reforge Stones, Gemstones, Drill Parts and more)")
    @Accordion
    public EstimatedItemValueConfig estimatedItemValues = new EstimatedItemValueConfig();

    @ConfigOption(name = "Discord Rich Presence", desc = "")
    @Accordion
    @Expose
    public DiscordRPCConfig discordRPC = new DiscordRPCConfig();

    @ConfigOption(name = "Trevor The Trapper", desc = "")
    @Accordion
    @Expose
    public TrevorTheTrapper trevorTheTrapper = new TrevorTheTrapper();

    public static class TrevorTheTrapper {

        @Expose
        @ConfigOption(
            name = "Enable Data Tracker",
            desc = "Tracks all of your data from doing Trevor Quests.\n" +
                "Shows based on the setting below."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean dataTracker = true;

        @Expose
        @ConfigOption(
            name = "Show Between Quests",
            desc = "Shows the tracker during and between quests otherwise it will only show during them." +
                "Will show in the Trapper's Den regardless. §cToggle 'Enable Data Tracker' above."
        )
        @ConfigEditorBoolean
        public boolean displayType = true;

        @Expose
        @ConfigOption(
            name = "Text Format",
            desc = "Drag text to change the appearance of the overlay."
        )
        @ConfigEditorDraggableList(
            exampleText = {
                "§b§lTrevor Data Tracker",
                "§b1,428 §9Quests Started",
                "§b11,281 §5Total Pelts Gained",
                "§b2,448 §5Pelts Per Hour",
                "",
                "§b850 §cKilled Animals",
                "§b153 §cSelf Killing Animals",
                "§b788 §fTrackable Animals",
                "§b239 §aUntrackable Animals",
                "§b115 §9Undetected Animals",
                "§b73 §5Endangered Animals",
                "§b12 §6Elusive Animals"
            }
        )
        public List<Integer> textFormat = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 7, 8, 9, 10, 11));

        @Expose
        public Position position = new Position(10, 80, false, true);

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
        @ConfigOption(name = "Accept Trapper Quest", desc = "Click this key after the chat prompt to accept Trevor's quest.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean acceptQuest = false;

        @Expose
        @ConfigOption(name = "Trapper Hotkey", desc = "Press this key to warp to Trevor's Den or to accept the quest. " +
            "§eRequires the relevant above settings to be toggled")
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
            @ConfigOption(name = "Line Color", desc = "Color of the line.")
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

        @Expose
        @ConfigOption(name = "Arrow Trail", desc = "")
        @Accordion
        public ArrowTrailConfig arrowTrailConfig = new ArrowTrailConfig();

        public static class ArrowTrailConfig {
            @Expose
            @ConfigOption(name = "Enabled", desc = "Draw a colored line behind arrows in the air.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = false;

            @Expose
            @ConfigOption(name = "Hide Nonplayer Arrows", desc = "Only shows for arrows the player has shot.")
            @ConfigEditorBoolean
            public boolean hideOtherArrows = true;

            @Expose
            @ConfigOption(name = "Arrow Color", desc = "Color of the line.")
            @ConfigEditorColour
            public String arrowColor = "0:200:85:255:85";

            @Expose
            @ConfigOption(name = "Player Arrows", desc = "Different color for the line of arrows that you have shot.")
            @ConfigEditorBoolean
            public boolean handlePlayerArrowsDifferently = false;

            @Expose
            @ConfigOption(name = "Player Arrow Color", desc = "Color of the line of your own arrows.")
            @ConfigEditorColour
            public String playerArrowColor = "0:200:85:255:255";

            @Expose
            @ConfigOption(name = "Time Alive", desc = "Time in seconds until the trail fades out.")
            @ConfigEditorSlider(minStep = 0.1f, minValue = 0.1f, maxValue = 10)
            public float secondsAlive = 0.5f;

            @Expose
            @ConfigOption(name = "Line Width", desc = "Width of the line.")
            @ConfigEditorSlider(minStep = 1, minValue = 1, maxValue = 10)
            public int lineWidth = 4;
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
    @ConfigOption(name = "Highlight Party Members", desc = "")
    @Accordion
    public HighlightPartyMembers highlightPartyMembers = new HighlightPartyMembers();

    public static class HighlightPartyMembers {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Marking party members with a bright outline to better find them in the world.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(
            name = "Outline Color",
            desc = "The color to outline party members in."
        )
        @ConfigEditorColour
        public String outlineColor = "0:245:85:255:85";

    }

    @Expose
    @ConfigOption(name = "Compact Tab List", desc = "")
    @Accordion
    public CompactTabListConfig compactTabList = new CompactTabListConfig();

    @Expose
    @ConfigOption(name = "Kick Duration", desc = "")
    @Accordion
    public KickDurationConfig kickDuration = new KickDurationConfig();

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
    @ConfigOption(name = "NEU Heavy Pearls", desc = "Fixing NEU Heavy Pearl detection.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean fixNeuHeavyPearls = true;

    @Expose
    @ConfigOption(
        name = "Time In Limbo",
        desc = "Show the time since you entered the limbo.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean showTimeInLimbo = true;

    @Expose
    public Position showTimeInLimboPosition = new Position(400, 200, 1.3f);

    @Expose
    public Position lockedMouseDisplay = new Position(400, 200, 0.8f);

    @Expose
    public Position inventoryLoadPos = new Position(394, 124, false, true);
}
