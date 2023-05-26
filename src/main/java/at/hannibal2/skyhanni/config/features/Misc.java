package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import io.github.moulberry.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

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
        public Position pos = new Position(394, 124, false, true);
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
    @ConfigOption(name = "Config Button", desc = "Add a button to the pause menu to configure SkyHanni.")
    @ConfigEditorBoolean
    public boolean configButtonOnPause = true;

    @Expose
    public Position inventoryLoadPos = new Position(394, 124, false, true);
}
