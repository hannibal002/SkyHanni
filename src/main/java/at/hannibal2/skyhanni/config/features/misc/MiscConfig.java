package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.features.misc.compacttablist.CompactTabListConfig;
import at.hannibal2.skyhanni.config.features.misc.cosmetic.CosmeticConfig;
import at.hannibal2.skyhanni.config.features.misc.pets.PetConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.Category;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class MiscConfig {

    @Expose
    @Category(name = "Pets", desc = "Pets Settings")
    public PetConfig pets = new PetConfig();

    @ConfigOption(name = "Hide Armor", desc = "")
    @Accordion
    @Expose
    // TOOD maybe we can migrate this already
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
    public TrevorTheTrapperConfig trevorTheTrapper = new TrevorTheTrapperConfig();

    @ConfigOption(name = "Teleport Pads On Private Island", desc = "")
    @Accordion
    @Expose
    public TeleportPadConfig teleportPad = new TeleportPadConfig();

    @ConfigOption(name = "Pocket Sack-In-A-Sack", desc = "")
    @Accordion
    @Expose
    public PocketSackInASackConfig pocketSackInASack = new PocketSackInASackConfig();

    @ConfigOption(name = "Quick Mod Menu Switch", desc = "")
    @Accordion
    @Expose
    public QuickModMenuSwitchConfig quickModMenuSwitch = new QuickModMenuSwitchConfig();

    @Expose
    @Category(name = "Cosmetic", desc = "Cosmetics Settings")
    public CosmeticConfig cosmetic = new CosmeticConfig();


    @Expose
    @ConfigOption(name = "Glowing Dropped Items", desc = "")
    @Accordion
    public GlowingDroppedItemsConfig glowingDroppedItems = new GlowingDroppedItemsConfig();

    @Expose
    @ConfigOption(name = "Highlight Party Members", desc = "")
    @Accordion
    public HighlightPartyMembersConfig highlightPartyMembers = new HighlightPartyMembersConfig();

    @Expose
    @Category(name = "Compact Tab List", desc = "Compact Tab List Settings")
    @Accordion
    public CompactTabListConfig compactTabList = new CompactTabListConfig();

    @Expose
    @ConfigOption(name = "Kick Duration", desc = "")
    @Accordion
    public KickDurationConfig kickDuration = new KickDurationConfig();

    @Expose
    @ConfigOption(name = "Tracker", desc = "Tracker Config")
    @Accordion
    public TrackerConfig tracker = new TrackerConfig();

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
    @ConfigOption(name = "Better Sign Editing", desc = "Allows pasting (Ctrl+V), copying (Ctrl+C), and deleting whole words/lines (Ctrl+Backspace/Ctrl+Shift+Backspace) in signs.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean betterSignEditing = true;

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
    @ConfigOption(
        name = "Lock Mouse Message",
        desc = "Show a message in chat when toggling the /shmouselock.")
    @ConfigEditorBoolean
    public boolean lockMouseLookChatMessage = true;

    @Expose
    public Position showTimeInLimboPosition = new Position(400, 200, 1.3f);

    @Expose
    public Position lockedMouseDisplay = new Position(400, 200, 0.8f);

    @Expose
    public Position inventoryLoadPos = new Position(394, 124, false, true);

    @Expose
    public int limboTimePB = 0;
}
