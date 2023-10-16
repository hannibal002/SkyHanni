package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

public class FishingConfig {

    @Expose
    @ConfigOption(name = "Trophy Fishing", desc = "")
    @Accordion
    public TrophyFishingConfig trophyFishing = new TrophyFishingConfig();

    public static class TrophyFishingConfig {

        @Expose
        @ConfigOption(name = "Trophy Fishing Chat Messages", desc = "")
        @Accordion
        public ChatMessagesConfig chatMessages = new ChatMessagesConfig();

        public static class ChatMessagesConfig {

            @Expose
            @ConfigOption(
                    name = "Trophy Counter",
                    desc = "Counts Trophy messages from chat and tells you how many you have found."
            )
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = false;

            @Expose
            @ConfigOption(
                    name = "Trophy Counter Design",
                    desc = "§fStyle 1: §72. §6§lGOLD §5Moldfin\n" +
                            "§fStyle 2: §bYou caught a §5Moldfin §6§lGOLD§b. §7(2)\n" +
                            "§fStyle 3: §bYou caught your 2nd §6§lGOLD §5Moldfin§b."
            )
            @ConfigEditorDropdown(values = {"Style 1", "Style 2", "Style 3"})
            public int design = 0;

            @Expose
            @ConfigOption(name = "Show Total Amount", desc = "Show total amount of all rarities at the end of the chat message.")
            @ConfigEditorBoolean
            public boolean totalAmount = false;

            @Expose
            @ConfigOption(name = "Trophy Fish Info", desc = "Show information and stats about a Trophy Fish when hovering over a catch message in chat.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean tooltip = true;

            @Expose
            @ConfigOption(name = "Hide Repeated Catches", desc = "Delete past catches of the same Trophy Fish from chat.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean duplicateHider = false;

            @Expose
            @ConfigOption(name = "Bronze Duplicates", desc = "Hide duplicate messages for bronze Trophy Fishes from chat.")
            @ConfigEditorBoolean
            public boolean bronzeHider = false;

            @Expose
            @ConfigOption(name = "Silver Duplicates", desc = "Hide duplicate messages for silver Trophy Fishes from chat.")
            @ConfigEditorBoolean
            public boolean silverHider = false;
        }

        @Expose
        @ConfigOption(name = "Fillet Tooltip", desc = "Show fillet value of Trophy Fish in tooltip.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean filletTooltip = true;

        @Expose
        @ConfigOption(name = "Odger Waypoint", desc = "Show the Odger waypoint when Trophy Fishes are in the inventory and no lava rod in hand.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean odgerLocation = true;
    }

    @Expose
    @ConfigOption(name = "Thunder Spark", desc = "")
    @Accordion
    public ThunderSparkConfig thunderSpark = new ThunderSparkConfig();
    public static class ThunderSparkConfig {
        @Expose
        @ConfigOption(name = "Thunder Spark Highlight", desc = "Highlight Thunder Sparks after killing a Thunder.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean highlight = false;

        @Expose
        @ConfigOption(name = "Thunder Spark Color", desc = "Color of the Thunder Sparks.")
        @ConfigEditorColour
        public String color = "0:255:255:255:255";
    }

    @Expose
    @ConfigOption(name = "Barn Fishing Timer", desc = "")
    @Accordion
    public BarnTimerConfig barnTimer = new BarnTimerConfig();
    public static class BarnTimerConfig{
        @Expose
        @ConfigOption(
                name = "Barn Fishing Timer",
                desc = "Show the time and amount of sea creatures while fishing on the barn via hub."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(
                name = "Worm Fishing",
                desc = "Show the Barn Fishing Timer even for worms or other sea creatures in the Crystal Hollows."
        )
        @ConfigEditorBoolean
        public boolean crystalHollows = true;

        @Expose
        @ConfigOption(
                name = "Stranded Fishing",
                desc = "Show the Barn Fishing Timer even on all the different islands Stranded players can visit."
        )
        @ConfigEditorBoolean
        public boolean forStranded = true;

        @Expose
        @ConfigOption(
                name = "Worm Cap Alert",
                desc = "Alerts you with sound if you hit the Worm Sea Creature limit of 60."
        )
        @ConfigEditorBoolean
        public boolean wormLimitAlert = true;

        @Expose
        @ConfigOption(name = "Reset Timer Hotkey", desc = "Press this key to reset the timer manualy")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int manualResetTimer = Keyboard.KEY_NONE;

        @Expose
        @ConfigOption(name = "Fishing Timer Alert", desc = "Change the amount of time in seconds until the timer dings.")
        @ConfigEditorSlider(
                minValue = 240,
                maxValue = 360,
                minStep = 10
        )
        public int alertTime = 330;

        @Expose
        public Position pos = new Position(10, 10, false, true);
    }

    @Expose
    @ConfigOption(name = "Chum/Chumcap Bucket Hider", desc = "")
    @Accordion
    public ChumBucketHiderConfig chumBucketHider = new ChumBucketHiderConfig();

    public static class ChumBucketHiderConfig {

        @Expose
        @ConfigOption(name = "Enable", desc = "Hide the Chum/Chumcap Bucket name tags for other players.")
        @ConfigEditorBoolean
        @FeatureToggle
        public Property<Boolean> enabled = Property.of(true);

        @Expose
        @ConfigOption(name = "Hide Bucket", desc = "Hide the Chum/Chumcap Bucket.")
        @ConfigEditorBoolean
        public Property<Boolean> hideBucket = Property.of(false);

        @Expose
        @ConfigOption(name = "Hide Own", desc = "Hides your own Chum/Chumcap Bucket.")
        @ConfigEditorBoolean
        public Property<Boolean> hideOwn = Property.of(false);
    }

    @Expose
    @ConfigOption(name = "Fished Item Name", desc = "")
    @Accordion
    public FishedItemNameConfig fishedItemName = new FishedItemNameConfig();

    public static class FishedItemNameConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the fished item name above the item when fishing.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Show Bait", desc = "Also show the name of the consumed bait.")
        @ConfigEditorBoolean
        public boolean showBaits = false;

    }

    @Expose
    @ConfigOption(name = "Fishing Hook Display", desc = "")
    @Accordion
    public FishingHookDisplayConfig fishingHookDisplay = new FishingHookDisplayConfig();

    public static class FishingHookDisplayConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Display the Hypixel timer until the fishing hook can be pulled out of the water/lava, only bigger and on your screen.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(
                name = "Hide Armor Stand",
                desc = "Hide the original armor stand from Hypixel when the SkyHanni display is enabled."
        )
        @ConfigEditorBoolean
        public boolean hideArmorStand = true;

        @Expose
        public Position position = new Position(460, -240, 3.4f);

        @Expose
        @ConfigOption(
                name = "Debug: Update Interval",
                desc = "Changes the time in ticks between updates (should be as high as possible). Default is 20"
        )
        @ConfigEditorSlider(
                minValue = 1,
                maxValue = 80,
                minStep = 1
        )
        public int debugUpdateInterval = 20;

        @Expose
        @ConfigOption(
                name = "Debug: Distance",
                desc = "Changes the maximal detection distance between the fishing rod bobber and " +
                        "the armor stand that shows the hypixel timer (should be as low as possible). Default is 0.1"
        )
        @ConfigEditorSlider(
                minValue = 0.01f,
                maxValue = 5f,
                minStep = 0.01f
        )
        public double debugMaxDistance = 0.1;
    }

    @Expose
    @ConfigOption(name = "Rare Sea Creatures", desc = "")
    @Accordion
    public RareCatches rareCatches = new RareCatches();

    public static class RareCatches {

        @Expose
        @ConfigOption(name = "Alert (Own Sea Creatures)", desc = "Show an alert on screen when you catch a rare sea creature.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean alertOwnCatches = true;

        @Expose
        @ConfigOption(name = "Alert (Other Sea Creatures)", desc = "Show an alert on screen when other players nearby catch a rare sea creature.")
        @ConfigEditorBoolean
        public boolean alertOtherCatches = false;

        @Expose
        @ConfigOption(name = "Play Sound Alert", desc = "Play a sound effect when rare sea creature alerts are displayed.")
        @ConfigEditorBoolean
        public boolean playSound = true;

        @Expose
        @ConfigOption(name = "Highlight", desc = "Highlight nearby rare sea creatures.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean highlight = false;

    }

    @Expose
    @ConfigOption(
            name = "Shark Fish Counter",
            desc = "Counts how many Sharks have been caught."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean sharkFishCounter = false;

    @Expose
    public Position sharkFishCounterPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Shorten Fishing Message", desc = "Shortens the chat message that says what type of Sea Creature you have fished.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean shortenFishingMessage = false;

    @Expose
    @ConfigOption(name = "Compact Double Hook", desc = "Adds Double Hook to the Sea Creature chat message instead of in a previous line.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean compactDoubleHook = true;
}
