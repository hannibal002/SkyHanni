package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigAccordionId;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventConfig {

    @ConfigOption(name = "Monthly Bingo", desc = "")
    @Accordion
    @Expose
    public BingoConfig bingo = new BingoConfig();

    public static class BingoConfig {

        @Expose
        @ConfigOption(name = "Bingo Card", desc = "")
        @Accordion
        public BingoCard bingoCard = new BingoCard();

        public static class BingoCard {
            @Expose
            @ConfigOption(name = "Enable", desc = "Displays the Bingo Card.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = true;
            @Expose
            @ConfigOption(name = "Quick Toggle", desc = "Quickly toggle the Bingo Card or the step helper by sneaking with SkyBlock Menu in hand.")
            @ConfigEditorBoolean
            public boolean quickToggle = true;

            @Expose
            @ConfigOption(name = "Bingo Steps", desc = "Show help with the next step in Bingo instead of the Bingo Card. " +
                    "§cThis feature is in early development. Expect bugs and missing goals.")
            @ConfigEditorBoolean
            public boolean stepHelper = false;

            @Expose
            @ConfigOption(name = "Hide Community Goals", desc = "Hide Community Goals from the Bingo Card display.")
            @ConfigEditorBoolean
            public Property<Boolean> hideCommunityGoals = Property.of(false);

            @Expose
            @ConfigOption(
                    name = "Show Guide",
                    desc = "Show tips and difficulty for bingo goals inside the Bingo Card inventory.\n" +
                            "These tips are made from inspirations and guides from the community,\n"+
                            "aiming to help you to complete the bingo card."
            )
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean bingoSplashGuide = true;

            @Expose
            public Position bingoCardPos = new Position(10, 10, false, true);
        }

        @Expose
        @ConfigOption(name = "Compact Chat Messages", desc = "")
        @Accordion
        public CompactChat compactChat = new CompactChat();

        public static class CompactChat {

            @Expose
            @ConfigOption(name = "Enable", desc = "Shortens chat messages about skill level ups, collection gains, " +
                    "new area discoveries and SkyBlock level up messages while on Bingo.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = true;

            @Expose
            @ConfigOption(name = "Hide Border", desc = "Hide the border messages before and after the compact level up messages.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean hideBorder = true;

            @Expose
            @ConfigOption(name = "Outside Bingo", desc = "Compact the level up chat messages outside of an Bingo profile as well.")
            @ConfigEditorBoolean
            public boolean outsideBingo = false;
        }

        @Expose
        @ConfigOption(name = "Minion Craft Helper", desc = "Show how many more items you need to upgrade the minion in your inventory. Especially useful for Bingo.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean minionCraftHelperEnabled = true;

        @Expose
        public Position minionCraftHelperPos = new Position(10, 10, false, true);
    }

    @ConfigOption(name = "Diana's Mythological Burrows", desc = "")
    @Accordion
    @Expose
    public DianaConfig diana = new DianaConfig();

    public static class DianaConfig {


        @Expose
        @ConfigOption(name = "Soopy Guess", desc = "Uses §eSoopy's Guess Logic §7to find the next burrow. Does not require SoopyV2 or ChatTriggers to be installed.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean burrowsSoopyGuess = false;

        @Expose
        @ConfigOption(name = "Nearby Detection", desc = "Show burrows near you.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean burrowsNearbyDetection = false;

        @Expose
        @ConfigOption(name = "Smooth Transition", desc = "Show the way from one burrow to another smoothly.")
        @ConfigEditorBoolean
        public boolean burrowSmoothTransition = false;

        @Expose
        @ConfigOption(name = "Nearest Warp", desc = "Warps to the nearest warp point on the hub, if closer to the next burrow.")
        @ConfigEditorBoolean
        public boolean burrowNearestWarp = false;

        @Expose
        @ConfigOption(name = "Warp Key", desc = "Press this key to warp to nearest burrow waypoint.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
        public int keyBindWarp = Keyboard.KEY_NONE;

        @Expose
        @ConfigOption(name = "Ignored Warps", desc = "")
        @Accordion
        public IgnoredWarpsConfig ignoredWarps = new IgnoredWarpsConfig();

        public static class IgnoredWarpsConfig {

            @Expose
            @ConfigOption(name = "Crypt", desc = "Ignore the Crypt warp point (Because it takes a long time to leave).")
            @ConfigEditorBoolean
            public boolean crypt = false;

            @Expose
            @ConfigOption(name = "Wizard", desc = "Ignore the Wizard Tower warp point (Because it is easy to fall into the rift).")
            @ConfigEditorBoolean
            public boolean wizard = false;

        }

        @Expose
        @ConfigOption(name = "Inquisitor Waypoint Sharing", desc = "")
        @Accordion
        @ConfigAccordionId(id = 9)
        public InquisitorSharing inquisitorSharing = new InquisitorSharing();

        public static class InquisitorSharing {

            @Expose
            @ConfigOption(name = "Enabled", desc = "Shares your Inquisitor and receiving other Inquisitors via Party Chat.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = true;

            @Expose
            @ConfigOption(name = "Focus", desc = "Hide other waypoints when your Party finds an Inquisitor.")
            @ConfigEditorBoolean
            public boolean focusInquisitor = false;

            @Expose
            @ConfigOption(name = "Instant Share", desc = "Share the waypoint as soon as you find an Inquisitor. As an alternative, you can share it only via key press.")
            @ConfigEditorBoolean
            public boolean instantShare = true;

            @Expose
            @ConfigOption(name = "Share Key", desc = "Press this key to share your Inquisitor Waypoint.")
            @ConfigEditorKeybind(defaultKey = Keyboard.KEY_Y)
            public int keyBindShare = Keyboard.KEY_Y;

            @Expose
            @ConfigOption(name = "Show Despawn Time", desc = "Show the time until the shared Inquisitor will despawn.")
            @ConfigEditorBoolean
            public boolean showDespawnTime = true;
        }

        @Expose
        @ConfigOption(name = "Griffin Pet Warning", desc = "Warn when holding an Ancestral Spade if a Griffin Pet is not equipped.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean petWarning = true;
    }

    @ConfigOption(name = "Winter Season on Jerry's Island", desc = "")
    @Accordion
    @Expose
    public WinterConfig winter = new WinterConfig();

    public static class WinterConfig {

        @Expose
        @ConfigOption(name = "Frozen Treasure Tracker", desc = "")
        @Accordion
        public FrozenTreasureConfig frozenTreasureTracker = new FrozenTreasureConfig();

        public static class FrozenTreasureConfig {

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
            @ConfigOption(name = "Show as Drops", desc = "Multiplies the numbers on the display by the base drop. \n" +
                    "E.g. 3 Ice Bait -> 48 Ice Bait")
            @ConfigEditorBoolean
            public boolean showAsDrops = false;

            @Expose
            @ConfigOption(name = "Hide Chat Messages", desc = "Hides the chat messages from Frozen Treasures.")
            @ConfigEditorBoolean
            public boolean hideMessages = false;

            @Expose
            public Position position = new Position(10, 80, false, true);
        }

        @Expose
        @ConfigOption(name = "Island Close Time", desc = "While on the Winter Island, show a timer until Jerry's Workshop closes.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean islandCloseTime = true;

        @Expose
        public Position islandCloseTimePosition = new Position(10, 10, false, true);

    }

    @ConfigOption(name = "City Project", desc = "")
    @Accordion
    @Expose
    public CityProjectConfig cityProject = new CityProjectConfig();

    public static class CityProjectConfig {

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

    @ConfigOption(name = "Mayor Jerry's Jerrypocalypse", desc = "")
    @Accordion
    @Expose
    public MayorJerryConfig jerry = new MayorJerryConfig();

    public static class MayorJerryConfig {

        @Expose
        @ConfigOption(name = "Highlight Jerries", desc = "Highlights Jerries found from the Jerrypocalypse perk. Highlight color is based on color of the Jerry.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean highlightJerries = true;

    }

    // comment in if the event is needed again
//    @ConfigOption(name = "300þ Anniversary Celebration", desc = "Features for the 300þ year of SkyBlock")
    @Accordion
    @Expose
    public Century century = new Century();

    public static class Century {

        @ConfigOption(name = "Enable Active Player Timer", desc = "Show a HUD telling you how much longer you have to wait to be eligible for another free ticket.")
        @Expose
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enableActiveTimer = true;

        @Expose
        public Position activeTimerPosition = new Position(100, 100, false, true);

        @ConfigOption(name = "Enable Active Player Alert", desc = "Loudly proclaim when it is time to break some wheat.")
        @Expose
        @ConfigEditorBoolean
        public boolean enableActiveAlert = false;
    }

    @Expose
    @ConfigOption(name = "Main Lobby Halloween Basket Waypoints", desc = "")
    @Accordion
    public halloweenBasketConfig halloweenBasket = new halloweenBasketConfig();

    public static class halloweenBasketConfig {

        @Expose
        @ConfigOption(name = "Basket Waypoints", desc = "Show all Halloween Basket waypoints.\nShoutout to §bTobbbb §7for the coordinates.\n(AS OF 2023)")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean allWaypoints = false;

        @Expose
        @ConfigOption(name = "Entrance Waypoints", desc = "Show helper waypoints to Baskets #23, #24, and #25. Coordinates by §bErymanthus§7.")
        @ConfigEditorBoolean
        public boolean allEntranceWaypoints = false;

        @Expose
        @ConfigOption(name = "Only Closest", desc = "Only show the closest waypoint")
        @ConfigEditorBoolean
        public boolean onlyClosest = true;
    }

}
