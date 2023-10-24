package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.commands.Commands;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.garden.inventory.GardenPlotIcon;
import at.hannibal2.skyhanni.utils.LorenzUtils;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GardenConfig {

    @Expose
    @ConfigOption(name = "SkyMart", desc = "")
    @Accordion
    public SkyMartConfig skyMart = new SkyMartConfig();

    public static class SkyMartConfig {
        @Expose
        @ConfigOption(name = "Copper Price", desc = "Show copper to coin prices inside the SkyMart inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean copperPrice = true;

        @Expose
        @ConfigOption(name = "Advanced Stats", desc = "Show the BIN price and copper price for every item.")
        @ConfigEditorBoolean
        public boolean copperPriceAdvancedStats = false;

        @Expose
        public Position copperPricePos = new Position(211, 132, false, true);
    }

    @Expose
    @ConfigOption(name = "Visitor", desc = "")
    @Accordion
    public VisitorConfig visitors = new VisitorConfig();

    public static class VisitorConfig {
        @Expose
        @ConfigOption(name = "Visitor Timer", desc = "")
        @Accordion
        public TimerConfig timer = new TimerConfig();

        public static class TimerConfig {
            @Expose
            @ConfigOption(name = "Visitor Timer", desc = "Timer when the next visitor will appear, " +
                    "and a number for how many visitors are already waiting.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = true;

            @Expose
            @ConfigOption(name = "Sixth Visitor Estimate", desc = "Estimate when the sixth visitor in the queue will arrive. " +
                    "May be inaccurate with co-op members farming simultaneously.")
            @ConfigEditorBoolean
            public boolean sixthVisitorEnabled = true;

            @Expose
            @ConfigOption(name = "Sixth Visitor Warning", desc = "Notifies when it is believed that the sixth visitor has arrived. " +
                "May be inaccurate with co-op members farming simultaneously.")
            @ConfigEditorBoolean
            public boolean sixthVisitorWarning = true;

            @Expose
            @ConfigOption(name = "Ping when close to new visitor", desc = "Pings you when you are less than 10 seconds away from getting a new visitor.")
            @ConfigEditorBoolean
            public boolean pingForVisitorArrivals = false;

            @Expose
            public Position pos = new Position(390, 65, false, true);
        }

        @Expose
        @ConfigOption(name = "Visitor Items Needed", desc = "")
        @Accordion
        public NeedsConfig needs = new NeedsConfig();

        public static class NeedsConfig {
            @Expose
            @ConfigOption(name = "Items Needed", desc = "Show all items needed for the visitors.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean display = true;

            @Expose
            public Position pos = new Position(180, 170, false, true);

            @Expose
            @ConfigOption(name = "Only when Close", desc = "Only show the needed items when close to the visitors.")
            @ConfigEditorBoolean
            public boolean onlyWhenClose = false;

            @Expose
            @ConfigOption(name = "Bazaar Alley", desc = "Show the Visitor Items List while inside the Bazaar Alley in the Hub. " +
                    "This helps buying the correct amount when not having a Booster Cookie Buff active.")
            @ConfigEditorBoolean
            public boolean inBazaarAlley = true;

            @Expose
            @ConfigOption(name = "Show Price", desc = "Show the coin price in the items needed list.")
            @ConfigEditorBoolean
            public boolean showPrice = true;

            @Expose
            @ConfigOption(name = "Item Preview", desc = "Show the base type for the required items next to new visitors. §cNote that some visitors may require any crop.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean itemPreview = true;
        }

        @Expose
        @ConfigOption(name = "Visitor Inventory", desc = "")
        @Accordion
        public InventoryConfig inventory = new InventoryConfig();

        public static class InventoryConfig {
            @Expose
            @ConfigOption(name = "Visitor Price", desc = "Show the Bazaar price of the items required for the visitors, like in NEU.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean showPrice = false;

            @Expose
            @ConfigOption(name = "Amount and Time", desc = "Show the exact item amount and the remaining time when farmed manually. Especially useful for Ironman.")
            @ConfigEditorBoolean
            public boolean exactAmountAndTime = true;

            @Expose
            @ConfigOption(name = "Copper Price", desc = "Show the price per copper inside the visitor GUI.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean copperPrice = true;

            @Expose
            @ConfigOption(name = "Copper Time", desc = "Show the time required per copper inside the visitor GUI.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean copperTime = false;

            @Expose
            @ConfigOption(name = "Garden Exp Price", desc = "Show the price per garden experience inside the visitor GUI.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean experiencePrice = false;
        }

        @Expose
        @ConfigOption(name = "Visitor Reward Warning", desc = "")
        @Accordion
        public RewardWarningConfig rewardWarning = new RewardWarningConfig();

        public static class RewardWarningConfig {

            @Expose
            @ConfigOption(name = "Notify in Chat", desc = "Send a chat message once you talk to a visitor with reward.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean notifyInChat = true;

            @Expose
            @ConfigOption(name = "Show over Name", desc = "Show the reward name above the visitor name.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean showOverName = true;

            @Expose
            @ConfigOption(name = "Prevent Refusing", desc = "Prevent the refusal of a visitor with reward.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean preventRefusing = true;

            @Expose
            @ConfigOption(name = "Bypass Key", desc = "Hold that key to bypass the Prevent Refusing feature.")
            @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
            public int bypassKey = Keyboard.KEY_NONE;


            /**
             * Sync up with {at.hannibal2.skyhanni.features.garden.visitor.VisitorReward}
             */
            @Expose
            @ConfigOption(
                    name = "Items",
                    desc = "Warn for these reward items."
            )
            @ConfigEditorDraggableList(
                    exampleText = {
                            "§9Flowering Bouquet",
                            "§9Overgrown Grass",
                            "§9Green Bandana",
                            "§9Dedication IV",
                            "§9Music Rune",
                            "§cSpace Helmet",
                            "§9Cultivating I",
                            "§9Replenish I",
                    }
            )
            public List<Integer> drops = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        }

        @Expose
        @ConfigOption(name = "Notification Chat", desc = "Show in chat when a new visitor is visiting your island.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean notificationChat = true;

        @Expose
        @ConfigOption(name = "Notification Title", desc = "Show a title when a new visitor is visiting your island.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean notificationTitle = true;

        @Expose
        @ConfigOption(name = "Highlight Status", desc = "Highlight the status for visitors with a text above or with color.")
        @ConfigEditorDropdown(values = {"Color Only", "Name Only", "Both", "Disabled"})
        public int highlightStatus = 2;

        @Expose
        @ConfigOption(name = "Colored Name", desc = "Show the visitor name in the color of the rarity.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean coloredName = true;

        @Expose
        @ConfigOption(name = "Hypixel Message", desc = "Hide the chat message from Hypixel that a new visitor has arrived at your garden.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hypixelArrivedMessage = true;

        @Expose
        @ConfigOption(name = "Hide Chat", desc = "Hide chat messages from the visitors in garden. (Except Beth and Spaceman)")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideChat = true;

        @Expose
        @ConfigOption(name = "Visitor Drops Statistics Counter", desc = "")
        @Accordion
        public DropsStatisticsConfig dropsStatistics = new DropsStatisticsConfig();

        public static class DropsStatisticsConfig {

            @Expose
            @ConfigOption(
                    name = "Enabled",
                    desc = "Tallies up statistic about visitors and the rewards you have received from them."
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
                            "§e§lVisitor Statistics",
                            "§e1,636 Total",
                            "§a1,172§f-§9382§f-§681§f-§c1",
                            "§21,382 Accepted",
                            "§c254 Denied",
                            " ",
                            "§c62,072 Copper",
                            "§33.2m Farming EXP",
                            "§647.2m Coins Spent",
                            "§b23 §9Flowering Bouquet",
                            "§b4 §9Overgrown Grass",
                            "§b2 §5Green Bandana",
                            "§b1 §9Dedication IV",
                            "§b6 §b◆ Music Rune I",
                            "§b1 §cSpace Helmet",
                            "§b1 §9Cultivating I",
                            "§b1 §9Replenish I",
                            " ", // If they want another empty row
                            "§212,600 Garden EXP",
                            "§b4.2k Bits",
                            "§220k Mithril Powder",
                            "§d18k Gemstone Powder",
                    }
            )
            public List<Integer> textFormat = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12));


            @Expose
            @ConfigOption(name = "Display Numbers First", desc = "Determines whether the number or drop name displays first. " +
                    "§eNote: Will not update the preview above!")
            @ConfigEditorBoolean
            public boolean displayNumbersFirst = true;

            @Expose
            @ConfigOption(name = "Display Icons", desc = "Replaces the drop names with icons. " +
                    "§eNote: Will not update the preview above!")
            @ConfigEditorBoolean
            public boolean displayIcons = false;

            @Expose
            @ConfigOption(name = "Only on Barn Plot", desc = "Only shows the overlay while on the Barn plot.")
            @ConfigEditorBoolean
            public boolean onlyOnBarn = true;

            @Expose
            public Position pos = new Position(5, 20, false, true);
        }

        @Expose
        @ConfigOption(
            name = "Accept Hotkey",
            desc = "Accept a visitor when you press this keybind while in the visitor GUI"
        )
        @ConfigEditorKeybind(
            defaultKey = Keyboard.KEY_NONE
        )
        public int acceptHotkey = Keyboard.KEY_NONE;
    }

    @Expose
    @ConfigOption(name = "Numbers", desc = "")
    @Accordion
    public NumbersConfig number = new NumbersConfig();

    public static class NumbersConfig {
        @Expose
        @ConfigOption(name = "Crop Milestone", desc = "Show the number of crop milestones in the inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean cropMilestone = true;

        @Expose
        @ConfigOption(name = "Average Milestone", desc = "Show the average crop milestone in the crop milestone inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean averageCropMilestone = true;

        @Expose
        @ConfigOption(name = "Crop Upgrades", desc = "Show the number of upgrades in the crop upgrades inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean cropUpgrades = true;

        @Expose
        @ConfigOption(name = "Composter Upgrades", desc = "Show the number of upgrades in the Composter upgrades inventory.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean composterUpgrades = true;
    }

    @Expose
    @ConfigOption(name = "Crop Milestones", desc = "")
    @Accordion
    public CropMilestonesConfig cropMilestones = new CropMilestonesConfig();

    public static class CropMilestonesConfig {
        @Expose
        @ConfigOption(
                name = "Progress Display",
                desc = "Shows the progress and ETA until the next crop milestone is reached and the current crops/minute value. " +
                        "§eRequires a tool with either a counter or Cultivating enchantment for full accuracy."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean progress = true;

        @Expose
        @ConfigOption(
                name = "Warn When Close",
                desc = "Warn with title and sound when the next crop milestone upgrade happens in 5 seconds. " +
                        "Useful for switching to a different pet for leveling.")
        @ConfigEditorBoolean
        public boolean warnClose = false;

        @Expose
        @ConfigOption(
                name = "Time Format",
                desc = "Change the highest time unit to show (1h30m vs 90min)")
        @ConfigEditorDropdown(values = {"Year", "Day", "Hour", "Minute", "Second"})
        public Property<Integer> highestTimeFormat = Property.of(0);

        @Expose
        @ConfigOption(
                name = "Maxed Milestone",
                desc = "Calculate the progress and ETA till maxed milestone (46) instead of next milestone.")
        @ConfigEditorBoolean
        public Property<Boolean> bestShowMaxedNeeded = Property.of(false);

        @Expose
        @ConfigOption(
                name = "Milestone Text",
                desc = "Drag text to change the appearance of the overlay.\n" +
                        "Hold a farming tool to show the overlay."
        )
        @ConfigEditorDraggableList(
                exampleText = {
                        "§6Crop Milestones",
                        "§7Pumpkin Tier 22",
                        "§e12,300§8/§e100,000",
                        "§7In §b12m 34s",
                        "§7Crops/Minute§8: §e12,345",
                        "§7Blocks/Second§8: §e19.85",
                        "§7Percentage: §e12.34%",
                }
        )
        public List<Integer> text = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5));

        @Expose
        @ConfigOption(name = "Block Broken Precision", desc = "The amount of decimals displayed in blocks/second.")
        @ConfigEditorSlider(
                minValue = 0,
                maxValue = 6,
                minStep = 1
        )
        public int blocksBrokenPrecision = 2;

        @Expose
        @ConfigOption(name = "Seconds Before Reset", desc = "How many seconds of not farming until blocks/second resets.")
        @ConfigEditorSlider(
                minValue = 2,
                maxValue = 60,
                minStep = 1
        )
        public int blocksBrokenResetTime = 5;

        @Expose
        public Position progressDisplayPos = new Position(-400, -200, false, true);

        @Expose
        @ConfigOption(name = "Best Crop", desc = "")
        @Accordion
        public NextConfig next = new NextConfig();

        // TODO moulconfig runnable support
        public static class NextConfig {
            @Expose
            @ConfigOption(
                    name = "Best Display",
                    desc = "Lists all crops and their ETA till next milestone. Sorts for best crop for getting garden or SkyBlock levels.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean bestDisplay = true;

            // TODO moulconfig runnable support
            @Expose
            @ConfigOption(name = "Sort Type", desc = "Sort the crops by either garden or SkyBlock EXP.")
            @ConfigEditorDropdown(values = {"Garden Exp", "SkyBlock Exp"})
            public int bestType = 0;

            // TODO moulconfig runnable support
            @Expose
            @ConfigOption(name = "Only Show Top", desc = "Only show the top # crops.")
            @ConfigEditorSlider(
                    minValue = 1,
                    maxValue = 10,
                    minStep = 1
            )
            public int showOnlyBest = 10;

            @Expose
            @ConfigOption(name = "Extend Top List", desc = "Add current crop to the list if its lower ranked than the set limit by extending the list.")
            @ConfigEditorBoolean
            public boolean showCurrent = true;

            // TODO moulconfig runnable support
            @Expose
            @ConfigOption(
                    name = "Always On",
                    desc = "Show the Best Display always while on the garden.")
            @ConfigEditorBoolean
            public boolean bestAlwaysOn = false;

            @Expose
            @ConfigOption(
                    name = "Compact Display",
                    desc = "A more compact best crop time: Removing the crop name and exp, hide the # number and using a more compact time format.")
            @ConfigEditorBoolean
            public boolean bestCompact = false;

            @Expose
            @ConfigOption(
                    name = "Hide Title",
                    desc = "Hides the 'Best Crop Time' line entirely.")
            @ConfigEditorBoolean
            public boolean bestHideTitle = false;


            @Expose
            public Position displayPos = new Position(-200, -200, false, true);
        }

        @Expose
        @ConfigOption(name = "Mushroom Pet Perk", desc = "")
        @Accordion
        public MushroomPetPerkConfig mushroomPetPerk = new MushroomPetPerkConfig();

        // TODO moulconfig runnable support
        public static class MushroomPetPerkConfig {
            @Expose
            @ConfigOption(
                    name = "Display Enabled",
                    desc = "Show the progress and ETA for mushroom crops when farming other crops because of the Mooshroom Cow perk.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = true;

            @Expose
            @ConfigOption(
                    name = "Mushroom Text",
                    desc = "Drag text to change the appearance of the overlay.\n" +
                            "Hold a farming tool to show the overlay."
            )
            @ConfigEditorDraggableList(
                    exampleText = {
                            "§6Mooshroom Cow Perk",
                            "§7Mushroom Tier 8",
                            "§e6,700§8/§e15,000",
                            "§7In §b12m 34s",
                            "§7Percentage: §e12.34%",
                    }
            )
            public List<Integer> text = new ArrayList<>(Arrays.asList(0, 1, 2, 3));

            @Expose
            public Position pos = new Position(-112, -143, false, true);
        }
    }

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Custom Keybinds", desc = "")
    @Accordion
    public KeyBindConfig keyBind = new KeyBindConfig();

    public static class KeyBindConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Use custom keybinds while holding a farming tool or Daedalus Axe in the hand.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @ConfigOption(name = "Disable All", desc = "Disable all keys.")
        @ConfigEditorButton(buttonText = "Disable")
        public Runnable presetDisable = () -> {
            attack = Keyboard.KEY_NONE;
            useItem = Keyboard.KEY_NONE;
            left = Keyboard.KEY_NONE;
            right = Keyboard.KEY_NONE;
            forward = Keyboard.KEY_NONE;
            back = Keyboard.KEY_NONE;
            jump = Keyboard.KEY_NONE;
            sneak = Keyboard.KEY_NONE;

            Minecraft.getMinecraft().thePlayer.closeScreen();
        };

        @ConfigOption(name = "Set Default", desc = "Reset all keys to default.")
        @ConfigEditorButton(buttonText = "Default")
        public Runnable presetDefault = () -> {
            attack = -100;
            useItem = -99;
            left = Keyboard.KEY_A;
            right = Keyboard.KEY_D;
            forward = Keyboard.KEY_W;
            back = Keyboard.KEY_S;
            jump = Keyboard.KEY_SPACE;
            sneak = Keyboard.KEY_LSHIFT;
            Minecraft.getMinecraft().thePlayer.closeScreen();
        };

        @Expose
        @ConfigOption(name = "Attack", desc = "")
        @ConfigEditorKeybind(defaultKey = -100)
        public int attack = -100;

        @Expose
        @ConfigOption(name = "Use Item", desc = "")
        @ConfigEditorKeybind(defaultKey = -99)
        public int useItem = -99;

        @Expose
        @ConfigOption(name = "Move Left", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
        public int left = Keyboard.KEY_A;

        @Expose
        @ConfigOption(name = "Move Right", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
        public int right = Keyboard.KEY_D;

        @Expose
        @ConfigOption(name = "Move Forward", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
        public int forward = Keyboard.KEY_W;

        @Expose
        @ConfigOption(name = "Move Back", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
        public int back = Keyboard.KEY_S;

        @Expose
        @ConfigOption(name = "Jump", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
        public int jump = Keyboard.KEY_SPACE;

        @Expose
        @ConfigOption(name = "Sneak", desc = "")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
        public int sneak = Keyboard.KEY_LSHIFT;
    }

    @Expose
    @ConfigOption(name = "Optimal Speed", desc = "")
    @Accordion
    public OptimalSpeedConfig optimalSpeeds = new OptimalSpeedConfig();

    public static class OptimalSpeedConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "Show the optimal speed for your current tool in the hand.\n" +
                "(Thanks MelonKingDE for the default values).")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Warning Title", desc = "Warn via title when you don't have the optimal speed.")
        @ConfigEditorBoolean
        public boolean warning = false;

        @Expose
        @ConfigOption(name = "Rancher Boots", desc = "Allows you to set the optimal speed in the Rancher Boots overlay by clicking on the presets.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean signEnabled = true;

        @Expose
        public Position signPosition = new Position(20, -195, false, true);

        @Expose
        @ConfigOption(name = "Custom Speed", desc = "Change the exact speed for every single crop.")
        @Accordion
        public CustomSpeedConfig customSpeed = new CustomSpeedConfig();

        public static class CustomSpeedConfig {

            @Expose
            @ConfigOption(name = "Wheat", desc = "Suggested farm speed:\n" +
                    "§e5 Blocks§7: §f✦ 93 speed\n" +
                    "§e4 Blocks§7: §f✦ 116 speed")
            @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
            public int wheat = 93;

            @Expose
            @ConfigOption(name = "Carrot", desc = "Suggested farm speed:\n" +
                    "§e5 Blocks§7: §f✦ 93 speed\n" +
                    "§e4 Blocks§7: §f✦ 116 speed")
            @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
            public int carrot = 93;

            @Expose
            @ConfigOption(name = "Potato", desc = "Suggested farm speed:\n" +
                    "§e5 Blocks§7: §f✦ 93 speed\n" +
                    "§e4 Blocks§7: §f✦ 116 speed")
            @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
            public int potato = 93;

            @Expose
            @ConfigOption(name = "Nether Wart", desc = "Suggested farm speed:\n" +
                    "§e5 Blocks§7: §f✦ 93 speed\n" +
                    "§e4 Blocks§7: §f✦ 116 speed")
            @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
            public int netherWart = 93;

            @Expose
            @ConfigOption(name = "Pumpkin", desc = "Suggested farm speed:\n" +
                    "§e3 Blocks§7: §f✦ 155 speed\n" +
                    "§e2 Blocks§7: §f✦ 265 §7or §f400 speed")
            @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
            public int pumpkin = 155;

            @Expose
            @ConfigOption(name = "Melon", desc = "Suggested farm speed:\n" +
                    "§e3 Blocks§7: §f✦ 155 speed\n" +
                    "§e2 Blocks§7: §f✦ 265 or 400 speed")
            @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
            public int melon = 155;

            @Expose
            @ConfigOption(name = "Cocoa Beans", desc = "Suggested farm speed:\n" +
                    "§e3 Blocks§7: §f✦ 155 speed\n" +
                    "§e4 Blocks§7: §f✦ 116 speed")
            @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
            public int cocoaBeans = 155;

            // TODO does other speed settings exist?
            @Expose
            @ConfigOption(name = "Sugar Cane", desc = "Suggested farm speed:\n" +
                    "§eYaw 45§7: §f✦ 328 speed")
            @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
            public int sugarCane = 328;

            @Expose
            @ConfigOption(name = "Cactus", desc = "Suggested farm speed:\n" +
                    "§eNormal§7: §f✦ 400 speed\n" +
                    "§eRacing Helmet§7: §f✦ 464 speed\n" +
                    "§eBlack Cat§7: §f✦ 464 speed")
            @ConfigEditorSlider(minValue = 1, maxValue = 500, minStep = 1)
            public int cactus = 400;

            // TODO does other speed settings exist?
            @Expose
            @ConfigOption(name = "Mushroom", desc = "Suggested farm speed:\n" +
                    "§eYaw 60§7: §f✦ 233 speed")
            @ConfigEditorSlider(minValue = 1, maxValue = 400, minStep = 1)
            public int mushroom = 233;
        }

        @Expose
        public Position pos = new Position(5, -200, false, true);
    }

    @Expose
    @ConfigOption(name = "Garden Level", desc = "")
    @Accordion
    public GardenLevelConfig gardenLevels = new GardenLevelConfig();

    public static class GardenLevelConfig {
        @Expose
        @ConfigOption(name = "Display", desc = "Show the current Garden level and progress to the next level.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean display = true;

        @Expose
        public Position pos = new Position(390, 40, false, true);
    }

    @Expose
    @ConfigOption(name = "Farming Weight", desc = "")
    @Accordion
    public EliteFarmingWeightConfig eliteFarmingWeights = new EliteFarmingWeightConfig();

    public static class EliteFarmingWeightConfig {
        @Expose
        @ConfigOption(name = "Display", desc = "Display your farming weight on screen. " +
                "The calculation and API is provided by The Elite SkyBlock farmers. " +
                "See §ehttps://elitebot.dev/info §7for more info.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean display = true;

        @Expose
        public Position pos = new Position(180, 10, false, true);

        @Expose
        @ConfigOption(name = "Leaderboard Ranking", desc = "Show your position in the farming weight leaderboard. " +
                "Only if your farming weight is high enough! Updates every 10 minutes.")
        @ConfigEditorBoolean
        public boolean leaderboard = true;

        @Expose
        @ConfigOption(name = "Overtake ETA", desc = "Show a timer estimating when you'll move up a spot in the leaderboard! " +
                "Will show an ETA to rank #10,000 if you're not on the leaderboard yet.")
        @ConfigEditorBoolean
        public boolean overtakeETA = false;

        @Expose
        @ConfigOption(name = "Offscreen Drop Message", desc = "Show a chat message when joining Garden how many spots you have dropped since last Garden join.")
        @ConfigEditorBoolean
        public boolean offScreenDropMessage = true;

        @Expose
        @ConfigOption(name = "Always ETA", desc = "Show the Overtake ETA always, even when not farming at the moment.")
        @ConfigEditorBoolean
        public boolean overtakeETAAlways = true;

        @Expose
        @ConfigOption(name = "ETA Goal", desc = "Override the Overtake ETA to show when you'll reach the specified rank (if not there yet). (Default: \"10,000\")")
        @ConfigEditorText
        public String ETAGoalRank = "10000";

        @Expose
        @ConfigOption(name = "Show below 200", desc = "Show the farming weight data even if you are below 200 weight.")
        @ConfigEditorBoolean
        public boolean ignoreLow = false;
    }

    @Expose
    @ConfigOption(name = "Dicer Counter", desc = "")
    @Accordion
    public DicerCounterConfig dicerCounters = new DicerCounterConfig();

    public static class DicerCounterConfig {
        @Expose
        @ConfigOption(name = "RNG Drop Counter", desc = "Count RNG drops for Melon Dicer and Pumpkin Dicer.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean display = true;

        @Expose
        @ConfigOption(name = "Hide Chat", desc = "Hide the chat message when dropping a RNG Dicer drop.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideChat = false;

        @Expose
        public Position pos = new Position(16, -232, false, true);
    }

    @Expose
    @ConfigOption(name = "Money per Hour", desc = "")
    @Accordion
    public MoneyPerHourConfig moneyPerHours = new MoneyPerHourConfig();

    public static class MoneyPerHourConfig {
        @Expose
        @ConfigOption(name = "Show Money per Hour",
                desc = "Displays the money per hour YOU get with YOUR crop/minute value when selling the item to bazaar. " +
                        "Supports Bountiful, Mushroom Cow Perk, Armor Crops and Dicer Drops. Their toggles are below.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean display = true;

        // TODO moulconfig runnable support
        @Expose
        @ConfigOption(name = "Only Show Top", desc = "Only show the best # items.")
        @ConfigEditorSlider(
                minValue = 1,
                maxValue = 25,
                minStep = 1
        )
        public int showOnlyBest = 5;

        @Expose
        @ConfigOption(name = "Extend Top List", desc = "Add current crop to the list if its lower ranked than the set limit by extending the list.")
        @ConfigEditorBoolean
        public boolean showCurrent = true;

        // TODO moulconfig runnable support
        @Expose
        @ConfigOption(
                name = "Always On",
                desc = "Always show the money/hour Display while on the garden.")
        @ConfigEditorBoolean
        public boolean alwaysOn = false;

        @Expose
        @ConfigOption(
                name = "Compact Mode",
                desc = "Hide the item name and the position number.")
        @ConfigEditorBoolean
        public boolean compact = false;

        @Expose
        @ConfigOption(
                name = "Compact Price",
                desc = "Show the price more compact.")
        @ConfigEditorBoolean
        public boolean compactPrice = false;

        @Expose
        @ConfigOption(
                name = "Use Custom",
                desc = "Use the custom format below instead of classic ➜ §eSell Offer §7and other profiles ➜ §eNPC Price.")
        @ConfigEditorBoolean
        public boolean useCustomFormat = false;

        @Expose
        @ConfigOption(
                name = "Custom Format",
                desc = "Set what prices to show")
        @ConfigEditorDraggableList(
                exampleText = {
                        "§eSell Offer",
                        "§eInstant Sell",
                        "§eNPC Price"
                },
                requireNonEmpty = true
        )
        public List<Integer> customFormat = new ArrayList<>(Arrays.asList(0, 1, 2));

        @Expose
        @ConfigOption(
                name = "Merge Seeds",
                desc = "Merge the seeds price with the wheat price.")
        @ConfigEditorBoolean
        public boolean mergeSeeds = true;

        @Expose
        @ConfigOption(
                name = "Include Bountiful",
                desc = "Includes the coins from Bountiful in the calculation.")
        @ConfigEditorBoolean
        public boolean bountiful = true;

        @Expose
        @ConfigOption(
                name = "Include Mooshroom Cow",
                desc = "Includes the coins you get from selling the mushrooms from your Mooshroom Cow pet.")
        @ConfigEditorBoolean
        public boolean mooshroom = true;

        @Expose
        @ConfigOption(
                name = "Include Armor Drops",
                desc = "Includes the average coins/hr from your armor.")
        @ConfigEditorBoolean
        public boolean armor = true;

        @Expose
        @ConfigOption(
                name = "Include Dicer Drops",
                desc = "Includes the average coins/hr from your melon or pumpkin dicer.")
        @ConfigEditorBoolean
        public boolean dicer = true;

        @Expose
        @ConfigOption(
                name = "Hide Title",
                desc = "Hides the first line of 'Money Per Hour' entirely.")
        @ConfigEditorBoolean
        public boolean hideTitle = false;

        @Expose
        public Position pos = new Position(-330, 170, false, true);
    }

    @Expose
    @ConfigOption(name = "Next Jacob's Contest", desc = "")
    @Accordion
    public NextJacobContestConfig nextJacobContests = new NextJacobContestConfig();

    public static class NextJacobContestConfig {
        @Expose
        @ConfigOption(name = "Show Jacob's Contest", desc = "Show the current or next Jacob's farming contest time and crops.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean display = true;

        @Expose
        @ConfigOption(name = "Outside Garden", desc = "Show the timer not only in Garden but everywhere in SkyBlock.")
        @ConfigEditorBoolean
        public boolean everywhere = false;

        @Expose
        @ConfigOption(name = "In Other Guis", desc = "Mark the current or next Farming Contest crops in other farming GUIs as underlined.")
        @ConfigEditorBoolean
        public boolean otherGuis = false;

        @Expose
        @ConfigOption(name = "Fetch Contests", desc = "Automatically fetch Contests from elitebot.dev for the current year if they're uploaded already.")
        @ConfigEditorBoolean
        public boolean fetchAutomatically = true;

        @Expose
        @ConfigOption(name = "Share Contests", desc = "Share the list of upcoming Contests to elitebot.dev for everyone else to then fetch automatically.")
        @ConfigEditorDropdown(values = {"Ask When Needed", "Share Automatically", "Disabled"})
        public int shareAutomatically = 0;

        @Expose
        @ConfigOption(name = "Warning", desc = "Show a warning shortly before a new Jacob's Contest starts.")
        @ConfigEditorBoolean
        public boolean warn = false;

        @Expose
        @ConfigOption(name = "Warning Time", desc = "Set the warning time in seconds before a Jacob's Contest begins.")
        @ConfigEditorSlider(
                minValue = 10,
                maxValue = 60 * 5,
                minStep = 1
        )
        public int warnTime = 60 * 2;

        @Expose
        @ConfigOption(name = "Popup Warning", desc = "Opens a popup when the warning time is reached and Minecraft is not in focus.")
        @ConfigEditorBoolean
        public boolean warnPopup = false;

        @Expose
        public Position pos = new Position(-200, 10, false, true);
    }

    @Expose
    @ConfigOption(name = "Farming Armor Drops", desc = "")

    @Accordion
    public FarmingArmorDropsConfig farmingArmorDrop = new FarmingArmorDropsConfig();

    public static class FarmingArmorDropsConfig {
        @Expose
        @ConfigOption(name = "Show Counter", desc = "Count all §9Cropie§7, §5Squash §7and §6Fermento §7dropped.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Hide Chat", desc = "Hide the chat message when receiving a farming armor drop.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean hideChat = false;

        @Expose
        public Position pos = new Position(16, -232, false, true);
    }

    @Expose
    @ConfigOption(name = "Anita Shop", desc = "")
    @Accordion
    public AnitaShopConfig anitaShop = new AnitaShopConfig();

    public static class AnitaShopConfig {
        @Expose
        @ConfigOption(
                name = "Medal Prices",
                desc = "Helps to identify profitable items to buy at the Anita item shop " +
                        "and potential profit from selling the item in the Auction House."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean medalProfitEnabled = true;

        @Expose
        @ConfigOption(
                name = "Extra Farming Fortune",
                desc = "Show current tier and cost to max out in the item tooltip.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean extraFarmingFortune = true;

        @Expose
        public Position medalProfitPos = new Position(206, 158, false, true);
    }

    @Expose
    @ConfigOption(name = "Composter", desc = "")
    @Accordion
    public ComposterConfig composters = new ComposterConfig();

    public static class ComposterConfig {
        @Expose
        @ConfigOption(
                name = "Composter Overlay",
                desc = "Show organic matter, fuel, and profit prices while inside the Composter Inventory."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean overlay = true;

        @Expose
        @ConfigOption(name = "Overlay Price", desc = "Toggle for Bazaar 'buy order' vs 'instant buy' price in composter overlay.")
        @ConfigEditorDropdown(values = {"Instant Buy", "Buy Order"})
        public int overlayPriceType = 0;

        @Expose
        @ConfigOption(name = "Retrieve From", desc = "Change where to retrieve the materials from in the composter overlay: The Bazaar or Sacks.")
        @ConfigEditorDropdown(values = {"Bazaar", "Sacks"})
        public int retrieveFrom = 0;

        @Expose
        public Position overlayOrganicMatterPos = new Position(140, 152, false, true);

        @Expose
        public Position overlayFuelExtrasPos = new Position(-320, 152, false, true);

        @Expose
        @ConfigOption(
                name = "Display Element",
                desc = "Displays the Compost data from the tab list as GUI element."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean displayEnabled = true;

        @Expose
        @ConfigOption(
                name = "Outside Garden",
                desc = "Show Time till Composter is empty outside Garden"
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean displayOutsideGarden = false;

        @Expose
        @ConfigOption(
                name = "Composter Warning",
                desc = "Warn when the Composter gets close to empty, even outside Garden."
        )
        @ConfigEditorBoolean
        public boolean warnAlmostClose = false;

        @Expose
        @ConfigOption(
                name = "Upgrade Price",
                desc = "Show the price for the Composter Upgrade in the lore."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean upgradePrice = true;

        @Expose
        @ConfigOption(
                name = "Round Amount Needed",
                desc = "Rounds the amount needed to fill your Composter down so that you don't overspend."
        )
        @ConfigEditorBoolean
        public boolean roundDown = true;

        @Expose
        @ConfigOption(
                name = "Highlight Upgrade",
                desc = "Highlight Upgrades that can be bought right now."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean highlightUpgrade = true;

        @Expose
        @ConfigOption(
                name = "Inventory Numbers",
                desc = "Show the amount of Organic Matter, Fuel and Composts Available while inside the Composter Inventory."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean inventoryNumbers = true;

        @Expose
        @ConfigOption(name = "Notification When Low Composter", desc = "")
        @Accordion
        public NotifyLowConfig notifyLow = new NotifyLowConfig();

        public static class NotifyLowConfig {
            @Expose
            @ConfigOption(name = "Enable", desc = "Show a notification when Organic Matter or Fuel runs low in your Composter.")
            @ConfigEditorBoolean
            @FeatureToggle
            public boolean enabled = false;

            @Expose
            @ConfigOption(name = "Show Title", desc = "Send a title to notify.")
            @ConfigEditorBoolean
            public boolean title = false;

            @Expose
            @ConfigOption(name = "Min Organic Matter", desc = "Warn when Organic Matter is below this value.")
            @ConfigEditorSlider(
                    minValue = 1_000,
                    maxValue = 80_000,
                    minStep = 1
            )
            public int organicMatter = 20_000;

            @Expose
            @ConfigOption(name = "Min Fuel Cap", desc = "Warn when Fuel is below this value.")
            @ConfigEditorSlider(
                    minValue = 500,
                    maxValue = 40_000,
                    minStep = 1
            )
            public int fuel = 10_000;
        }

        @Expose
        public Position displayPos = new Position(-390, 10, false, true);

        @Expose
        public Position outsideGardenPos = new Position(-363, 13, false, true);
    }

    @Expose
    @ConfigOption(name = "Farming Fortune Display", desc = "")
    @Accordion
    public FarmingFortuneConfig farmingFortunes = new FarmingFortuneConfig();

    public static class FarmingFortuneConfig {
        @Expose
        @ConfigOption(
                name = "FF Display",
                desc = "Displays the true Farming Fortune for the current crop, including all crop-specific and hidden bonuses."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean display = true;

        @Expose
        @ConfigOption(
                name = "Show As Drop Multiplier",
                desc = "Adds 100 to the displayed Farming Fortune so that it represents a drop multiplier rather than" +
                        " the chance for bonus drops. "
        )
        @ConfigEditorBoolean
        public boolean dropMultiplier = true;

        @ConfigOption(name = "Farming Fortune Guide", desc = "Opens a guide that breaks down your Farming Fortune.\n§eCommand: /ff")
        @ConfigEditorButton(buttonText = "Open")
        public Runnable open = Commands::openFortuneGuide;

        @Expose
        public Position pos = new Position(5, -180, false, true);
    }

    @Expose
    @ConfigOption(name = "Tooltip Tweaks", desc = "")
    @Accordion
    public TooltipTweaksConfig tooltipTweak = new TooltipTweaksConfig();

    public static class TooltipTweaksConfig {
        @Expose
        @ConfigOption(
                name = "Compact Descriptions",
                desc = "Hides redundant parts of reforge descriptions, generic counter description, and Farmhand perk explanation."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean compactToolTooltips = false;

        @Expose
        @ConfigOption(
                name = "Breakdown Hotkey",
                desc = "When the keybind is pressed, show a breakdown of all fortune sources on a tool."
        )
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
        public int fortuneTooltipKeybind = Keyboard.KEY_LSHIFT;

        @Expose
        @ConfigOption(
                name = "Tooltip Format",
                desc = "Show crop-specific Farming Fortune in tooltip.\n" +
                        "§fShow: §7Crop-specific Fortune indicated as §6[+196]\n" +
                        "§fReplace: §7Edits the total Fortune to include crop-specific Fortune."
        )
        @ConfigEditorDropdown(values = {"Default", "Show", "Replace"})
        public int cropTooltipFortune = 1;

        @Expose
        @ConfigOption(
                name = "Total Crop Milestone",
                desc = "Shows the progress bar till maxed crop milestone in the crop milestone inventory."
        )
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean cropMilestoneTotalProgress = true;
    }

    @Expose
    @ConfigOption(name = "Yaw and Pitch", desc = "")
    @Accordion
    public YawPitchDisplayConfig yawPitchDisplay = new YawPitchDisplayConfig();

    public static class YawPitchDisplayConfig {

        @Expose
        @ConfigOption(name = "Enable", desc = "Displays yaw and pitch while holding a farming tool. Automatically fades out if there is no movement.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

        @Expose
        @ConfigOption(name = "Yaw Precision", desc = "Yaw precision up to specified decimal.")
        @ConfigEditorSlider(
                minValue = 1,
                maxValue = 10,
                minStep = 1
        )
        public int yawPrecision = 4;

        @Expose
        @ConfigOption(name = "Pitch Precision", desc = "Pitch precision up to specified decimal.")
        @ConfigEditorSlider(
                minValue = 1,
                maxValue = 10,
                minStep = 1
        )
        public int pitchPrecision = 4;

        @Expose
        @ConfigOption(name = "Display Timeout", desc = "Duration in seconds for which the overlay is being displayed after moving.")
        @ConfigEditorSlider(
                minValue = 1,
                maxValue = 20,
                minStep = 1
        )
        public int timeout = 5;

        @Expose
        @ConfigOption(name = "Show Without Tool", desc = "Does not require you to hold a tool for the overlay to show.")
        @ConfigEditorBoolean
        public boolean showWithoutTool = false;

        @Expose
        @ConfigOption(name = "Show Outside Garden", desc = "The overlay will work outside of the Garden.")
        @ConfigEditorBoolean
        public boolean showEverywhere = false;

        @Expose
        @ConfigOption(name = "Ignore Timeout", desc = "Ignore the timeout after not moving mouse.")
        @ConfigEditorBoolean
        public boolean showAlways = false;

        @Expose
        public Position pos = new Position(445, 225, false, true);
        @Expose
        public Position posOutside = new Position(445, 225, false, true);
    }

    @Expose
    @ConfigOption(name = "Crop Start Location", desc = "")
    @Accordion
    public CropStartLocationConfig cropStartLocation = new CropStartLocationConfig();

    public static class CropStartLocationConfig {

        @Expose
        @ConfigOption(name = "Enable", desc = "Show the start waypoint for your farm with the currently holding tool.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = false;

    }

    @Expose
    @ConfigOption(name = "Garden Plot Icon", desc = "")
    @Accordion
    public PlotIconConfig plotIcon = new PlotIconConfig();

    public static class PlotIconConfig {
        @Expose
        @ConfigOption(name = "Enable", desc = "Enable icon replacement in the Configure Plots menu.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @ConfigOption(name = "Hard Reset", desc = "Reset every slot to its original item.")
        @ConfigEditorButton(buttonText = "Reset")
        public Runnable hardReset = () -> {
            GardenPlotIcon.INSTANCE.setHardReset(true);
            LorenzUtils.INSTANCE.sendCommandToServer("desk");
        };
    }

    @Expose
    @ConfigOption(name = "Plot Price", desc = "Show the price of the plot in coins when inside the Configure Plots inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean plotPrice = true;

    @Expose
    @ConfigOption(name = "Desk in Menu", desc = "Show a Desk button in the SkyBlock Menu. Opens the /desk command on click.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean deskInSkyBlockMenu = true;


    @Expose
    @ConfigOption(name = "Fungi Cutter Warning", desc = "Warn when breaking mushroom with the wrong Fungi Cutter mode.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean fungiCutterWarn = true;

    @Expose
    @ConfigOption(name = "Burrowing Spores", desc = "Show a notification when a Burrowing Spores spawns while farming mushrooms.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean burrowingSporesNotification = true;

    @Expose
    @ConfigOption(name = "Wild Strawberry", desc = "Show a notification when a Wild Strawberry Dye drops while farming.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean wildStrawberryDyeNotification = true;

    @Expose
    @ConfigOption(
            name = "FF for Contest",
            desc = "Show the minimum needed Farming Fortune for reaching each medal in Jacob's Farming Contest inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean farmingFortuneForContest = true;

    @Expose
    public Position farmingFortuneForContestPos = new Position(180, 156, false, true);

    @Expose
    @ConfigOption(
            name = "Contest Time Needed",
            desc = "Show the time and missing FF for every crop inside Jacob's Farming Contest inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean jacobContextTimes = true;

    @Expose
    public Position jacobContextTimesPos = new Position(-359, 149, false, true);

    @Expose
    @ConfigOption(
            name = "Contest Summary",
            desc = "Show the average Blocks Per Second and blocks clicked at the end of a Jacob Farming Contest in chat."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean jacobContestSummary = true;

    @Expose
    @ConfigOption(name = "Always Finnegan", desc = "Forcefully set the Finnegan Farming Simulator perk to be active. This is useful if the auto mayor detection fails.")
    @ConfigEditorBoolean
    public boolean forcefullyEnabledAlwaysFinnegan = false;

    @Expose
    public Position cropSpeedMeterPos = new Position(278, -236, false, true);

    @Expose
    @ConfigOption(name = "Enable Plot Borders", desc = "Enable the use of F3 + G hotkey to show Garden plot borders. Similar to how later Minecraft version render chunk borders.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean plotBorders = true;
}
