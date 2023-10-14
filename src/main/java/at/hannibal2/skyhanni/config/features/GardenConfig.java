package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.commands.Commands;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.garden.inventory.GardenPlotIcon;
import at.hannibal2.skyhanni.utils.LorenzUtils;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import io.github.moulberry.moulconfig.observer.Property;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
public class GardenConfig {

    @Expose
    @ConfigOption(name = "SkyMart", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean skyMart = false;

    @Expose
    @ConfigOption(name = "Copper Price", desc = "Show copper to coin prices inside the SkyMart inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean skyMartCopperPrice = true;

    @Expose
    @ConfigOption(name = "Advanced Stats", desc = "Show the BIN price and copper price for every item.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean skyMartCopperPriceAdvancedStats = false;

    @Expose
    public Position skyMartCopperPricePos = new Position(211, 132, false, true);

    @Expose
    @ConfigOption(name = "Visitor", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean visitor = false;

    @Expose
    @ConfigOption(name = "Visitor Timer", desc = "")
    @ConfigAccordionId(id = 1)
    @ConfigEditorAccordion(id = 2)
    public boolean visitorTimer = false;

    @Expose
    @ConfigOption(name = "Visitor Timer", desc = "Timer when the next visitor will appear, " +
            "and a number for how many visitors are already waiting.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    @FeatureToggle
    public boolean visitorTimerEnabled = true;

    @Expose
    @ConfigOption(name = "Sixth Visitor Estimate", desc = "Estimate when the sixth visitor in the queue will arrive. " +
            "May be inaccurate with co-op members farming simultaneously.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean visitorTimerSixthVisitorEnabled = true;

    @Expose
    @ConfigOption(name = "Sixth Visitor Warning", desc = "Notifies when it is believed that the sixth visitor has arrived. " +
            "May be inaccurate with co-op members farming simultaneously.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean visitorTimerSixthVisitorWarning = true;

    @Expose
    public Position visitorTimerPos = new Position(390, 65, false, true);

    @Expose
    @ConfigOption(name = "Visitor Items Needed", desc = "")
    @ConfigAccordionId(id = 1)
    @ConfigEditorAccordion(id = 3)
    public boolean visitorNeeds = false;

    @Expose
    @ConfigOption(name = "Items Needed", desc = "Show all items needed for the visitors.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean visitorNeedsDisplay = true;

    @Expose
    public Position visitorNeedsPos = new Position(180, 170, false, true);

    @Expose
    @ConfigOption(name = "Only when Close", desc = "Only show the needed items when close to the visitors.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean visitorNeedsOnlyWhenClose = false;

    @Expose
    @ConfigOption(name = "Bazaar Alley", desc = "Show the Visitor Items List while inside the Bazaar Alley in the Hub. " +
            "This helps buying the correct amount when not having a Booster Cookie Buff active.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean visitorNeedsInBazaarAlley = true;

    @Expose
    @ConfigOption(name = "Show Price", desc = "Show the coin price in the items needed list.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean visitorNeedsShowPrice = true;

    @Expose
    @ConfigOption(name = "Item Preview", desc = "Show the base type for the required items next to new visitors. §cNote that some visitors may require any crop.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    @FeatureToggle
    public boolean visitorItemPreview = true;

    @Expose
    @ConfigOption(name = "Visitor Inventory", desc = "")
    @ConfigAccordionId(id = 1)
    @ConfigEditorAccordion(id = 4)
    public boolean visitorInventory = false;

    @Expose
    @ConfigOption(name = "Visitor Price", desc = "Show the Bazaar price of the items required for the visitors, like in NEU.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    @FeatureToggle
    public boolean visitorShowPrice = false;

    @Expose
    @ConfigOption(name = "Amount and Time", desc = "Show the exact item amount and the remaining time when farmed manually. Especially useful for Ironman.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean visitorExactAmountAndTime = true;

    @Expose
    @ConfigOption(name = "Copper Price", desc = "Show the price per copper inside the visitor GUI.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    @FeatureToggle
    public boolean visitorCopperPrice = true;

    @Expose
    @ConfigOption(name = "Copper Time", desc = "Show the time required per copper inside the visitor GUI.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    @FeatureToggle
    public boolean visitorCopperTime = false;

    @Expose
    @ConfigOption(name = "Garden Exp Price", desc = "Show the price per garden experience inside the visitor GUI.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    @FeatureToggle
    public boolean visitorExperiencePrice = false;

    @Expose
    @ConfigOption(name = "Visitor Reward Warning", desc = "")
    @ConfigAccordionId(id = 1)
    @Accordion
    public VisitorRewardWarning visitorRewardWarning = new VisitorRewardWarning();

    public static class VisitorRewardWarning {

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
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean visitorNotificationChat = true;

    @Expose
    @ConfigOption(name = "Notification Title", desc = "Show a title when a new visitor is visiting your island.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean visitorNotificationTitle = true;

    @Expose
    @ConfigOption(name = "Highlight Status", desc = "Highlight the status for visitors with a text above or with color.")
    @ConfigEditorDropdown(values = {"Color Only", "Name Only", "Both", "Disabled"})
    @ConfigAccordionId(id = 1)
    public int visitorHighlightStatus = 2;

    @Expose
    @ConfigOption(name = "Colored Name", desc = "Show the visitor name in the color of the rarity.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean visitorColoredName = true;

    @Expose
    @ConfigOption(name = "Hypixel Message", desc = "Hide the chat message from Hypixel that a new visitor has arrived at your garden.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean visitorHypixelArrivedMessage = true;

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide chat messages from the visitors in garden. (Except Beth and Spaceman)")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    @FeatureToggle
    public boolean visitorHideChat = true;

    @Expose
    @ConfigOption(name = "Visitor Drops Statistics Counter", desc = "")
    @Accordion
    public VisitorDrops visitorDropsStatistics = new VisitorDrops();

    public static class VisitorDrops {

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
                        "§b2 §9Green Bandana",
                        "§b1 §9Dedication IV",
                        "§b6 §9Music Rune",
                        "§b1 §cSpace Helmet",
                        "§b1 §9Cultivating I",
                        "§b1 §9Replenish I",
                        " ", // If they want another empty row
                        "§212,735 Garden EXP",
                        "§b11,056 Bits",
                        "§250,556 Mithril Powder",
                        "§d50,556 Gemstone Powder",
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
        public Position visitorDropPos = new Position(5, 20, false, true);
    }

    @Expose
    @ConfigOption(name = "Numbers", desc = "")
    @ConfigEditorAccordion(id = 5)
    public boolean numbers = false;

    @Expose
    @ConfigOption(name = "Crop Milestone", desc = "Show the number of crop milestones in the inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    @FeatureToggle
    public boolean numberCropMilestone = true;

    @Expose
    @ConfigOption(name = "Average Milestone", desc = "Show the average crop milestone in the crop milestone inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    @FeatureToggle
    public boolean numberAverageCropMilestone = true;

    @Expose
    @ConfigOption(name = "Crop Upgrades", desc = "Show the number of upgrades in the crop upgrades inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    @FeatureToggle
    public boolean numberCropUpgrades = true;

    @Expose
    @ConfigOption(name = "Composter Upgrades", desc = "Show the number of upgrades in the Composter upgrades inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    @FeatureToggle
    public boolean numberComposterUpgrades = true;

    @Expose
    @ConfigOption(name = "Crop Milestones", desc = "")
    @ConfigEditorAccordion(id = 6)
    public boolean cropMilestones = false;

    @Expose
    @ConfigOption(
            name = "Progress Display",
            desc = "Shows the progress and ETA until the next crop milestone is reached and the current crops/minute value. " +
                    "§eRequires a tool with either a counter or Cultivating enchantment for full accuracy."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    @FeatureToggle
    public boolean cropMilestoneProgress = true;

    @Expose
    @ConfigOption(
            name = "Warn When Close",
            desc = "Warn with title and sound when the next crop milestone upgrade happens in 5 seconds. " +
                    "Useful for switching to a different pet for leveling.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean cropMilestoneWarnClose = false;

    @Expose
    @ConfigOption(
            name = "Time Format",
            desc = "Change the highest time unit to show (1h30m vs 90min)")
    @ConfigEditorDropdown(values = {"Year", "Day", "Hour", "Minute", "Second"})
    @ConfigAccordionId(id = 6)
    public Property<Integer> cropMilestoneHighestTimeFormat = Property.of(0);

    @Expose
    @ConfigOption(
            name = "Maxed Milestone",
            desc = "Calculate the progress and ETA till maxed milestone (46) instead of next milestone.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public Property<Boolean> cropMilestoneBestShowMaxedNeeded = Property.of(false);

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
    @ConfigAccordionId(id = 6)
    public List<Integer> cropMilestoneText = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5));

    @Expose
    @ConfigOption(name = "Block Broken Precision", desc = "The amount of decimals displayed in blocks/second.")
    @ConfigEditorSlider(
            minValue = 0,
            maxValue = 6,
            minStep = 1
    )
    @ConfigAccordionId(id = 6)
    public int blocksBrokenPrecision = 2;

    @Expose
    @ConfigOption(name = "Seconds Before Reset", desc = "How many seconds of not farming until blocks/second resets.")
    @ConfigEditorSlider(
            minValue = 2,
            maxValue = 60,
            minStep = 1
    )
    @ConfigAccordionId(id = 6)
    public int blocksBrokenResetTime = 5;

    @Expose
    public Position cropMilestoneProgressDisplayPos = new Position(-400, -200, false, true);

    @Expose
    @ConfigOption(name = "Best Crop", desc = "")
    @ConfigAccordionId(id = 6)
    @ConfigEditorAccordion(id = 7)
    public boolean cropMilestoneNext = false;
    // TODO moulconfig runnable support

    @Expose
    @ConfigOption(
            name = "Best Display",
            desc = "Lists all crops and their ETA till next milestone. Sorts for best crop for getting garden or SkyBlock levels.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    @FeatureToggle
    public boolean cropMilestoneBestDisplay = true;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Sort Type", desc = "Sort the crops by either garden or SkyBlock EXP.")
    @ConfigEditorDropdown(values = {"Garden Exp", "SkyBlock Exp"})
    @ConfigAccordionId(id = 7)
    public int cropMilestoneBestType = 0;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Only Show Top", desc = "Only show the top # crops.")
    @ConfigEditorSlider(
            minValue = 1,
            maxValue = 10,
            minStep = 1
    )
    @ConfigAccordionId(id = 7)
    public int cropMilestoneShowOnlyBest = 10;

    @Expose
    @ConfigOption(name = "Extend Top List", desc = "Add current crop to the list if its lower ranked than the set limit by extending the list.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    public boolean cropMilestoneShowCurrent = true;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(
            name = "Always On",
            desc = "Show the Best Display always while on the garden.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    public boolean cropMilestoneBestAlwaysOn = false;

    @Expose
    @ConfigOption(
            name = "Compact Display",
            desc = "A more compact best crop time: Removing the crop name and exp, hide the # number and using a more compact time format.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    public boolean cropMilestoneBestCompact = false;

    @Expose
    @ConfigOption(
            name = "Hide Title",
            desc = "Hides the 'Best Crop Time' line entirely.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    public boolean cropMilestoneBestHideTitle = false;


    @Expose
    public Position cropMilestoneNextDisplayPos = new Position(-200, -200, false, true);

    @Expose
    @ConfigOption(name = "Mushroom Pet Perk", desc = "")
    @ConfigAccordionId(id = 6)
    @ConfigEditorAccordion(id = 15)
    public boolean cropMilestoneMushroomPetPerk = false;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(
            name = "Display Enabled",
            desc = "Show the progress and ETA for mushroom crops when farming other crops because of the Mooshroom Cow perk.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 15)
    @FeatureToggle
    public boolean cropMilestoneMushroomPetPerkEnabled = true;

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
    @ConfigAccordionId(id = 15)
    public List<Integer> cropMilestoneMushroomPetPerkText = new ArrayList<>(Arrays.asList(0, 1, 2, 3));

    @Expose
    public Position cropMilestoneMushroomPetPerkPos = new Position(-112, -143, false, true);

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Custom Keybind", desc = "")
    @ConfigEditorAccordion(id = 8)
    public boolean keybind = false;

    @Expose
    @ConfigOption(name = "Enabled", desc = "Use custom keybinds while holding a farming tool or Daedalus Axe in the hand.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 8)
    @FeatureToggle
    public boolean keyBindEnabled = false;

    @ConfigOption(name = "Disable All", desc = "Disable all keys.")
    @ConfigEditorButton(buttonText = "Disable")
    @ConfigAccordionId(id = 8)
    public Runnable keyBindPresetDisable = () -> {
        keyBindAttack = Keyboard.KEY_NONE;
        keyBindUseItem = Keyboard.KEY_NONE;
        keyBindLeft = Keyboard.KEY_NONE;
        keyBindRight = Keyboard.KEY_NONE;
        keyBindForward = Keyboard.KEY_NONE;
        keyBindBack = Keyboard.KEY_NONE;
        keyBindJump = Keyboard.KEY_NONE;
        keyBindSneak = Keyboard.KEY_NONE;

        Minecraft.getMinecraft().thePlayer.closeScreen();
    };

    @ConfigOption(name = "Set Default", desc = "Reset all keys to default.")
    @ConfigEditorButton(buttonText = "Default")
    @ConfigAccordionId(id = 8)
    public Runnable keyBindPresetDefault = () -> {
        keyBindAttack = -100;
        keyBindUseItem = -99;
        keyBindLeft = Keyboard.KEY_A;
        keyBindRight = Keyboard.KEY_D;
        keyBindForward = Keyboard.KEY_W;
        keyBindBack = Keyboard.KEY_S;
        keyBindJump = Keyboard.KEY_SPACE;
        keyBindSneak = Keyboard.KEY_LSHIFT;
        Minecraft.getMinecraft().thePlayer.closeScreen();
    };

    @Expose
    @ConfigOption(name = "Attack", desc = "")
    @ConfigAccordionId(id = 8)
    @ConfigEditorKeybind(defaultKey = -100)
    public int keyBindAttack = -100;

    @Expose
    @ConfigOption(name = "Use Item", desc = "")
    @ConfigAccordionId(id = 8)
    @ConfigEditorKeybind(defaultKey = -99)
    public int keyBindUseItem = -99;

    @Expose
    @ConfigOption(name = "Move Left", desc = "")
    @ConfigAccordionId(id = 8)
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int keyBindLeft = Keyboard.KEY_A;

    @Expose
    @ConfigOption(name = "Move Right", desc = "")
    @ConfigAccordionId(id = 8)
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int keyBindRight = Keyboard.KEY_D;

    @Expose
    @ConfigOption(name = "Move Forward", desc = "")
    @ConfigAccordionId(id = 8)
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_W)
    public int keyBindForward = Keyboard.KEY_W;

    @Expose
    @ConfigOption(name = "Move Back", desc = "")
    @ConfigAccordionId(id = 8)
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_S)
    public int keyBindBack = Keyboard.KEY_S;

    @Expose
    @ConfigOption(name = "Jump", desc = "")
    @ConfigAccordionId(id = 8)
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_SPACE)
    public int keyBindJump = Keyboard.KEY_SPACE;

    @Expose
    @ConfigOption(name = "Sneak", desc = "")
    @ConfigAccordionId(id = 8)
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    public int keyBindSneak = Keyboard.KEY_LSHIFT;

    @Expose
    @ConfigOption(name = "Optimal Speed", desc = "")
    @ConfigEditorAccordion(id = 9)
    public boolean optimalSpeed = false;

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the optimal speed for your current tool in the hand.\n" +
            "(Thanks MelonKingDE for the default values).")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    @FeatureToggle
    public boolean optimalSpeedEnabled = true;

    @Expose
    @ConfigOption(name = "Warning Title", desc = "Warn via title when you don't have the optimal speed.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean optimalSpeedWarning = false;

    @Expose
    @ConfigOption(name = "Rancher Boots", desc = "Allows you to set the optimal speed in the Rancher Boots overlay by clicking on the presets.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    @FeatureToggle
    public boolean optimalSpeedSignEnabled = true;

    @Expose
    public Position optimalSpeedSignPosition = new Position(20, -195, false, true);

    @Expose
    @ConfigOption(name = "Custom Speed", desc = "Change the exact speed for every single crop.")
    @Accordion
    @ConfigAccordionId(id = 9)
    public CustomSpeed optimalSpeedCustom = new CustomSpeed();

    public static class CustomSpeed {

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
    public Position optimalSpeedPos = new Position(5, -200, false, true);

    @Expose
    @ConfigOption(name = "Garden Level", desc = "")
    @ConfigEditorAccordion(id = 10)
    public boolean gardenLevel = false;

    @Expose
    @ConfigOption(name = "Display", desc = "Show the current Garden level and progress to the next level.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 10)
    @FeatureToggle
    public boolean gardenLevelDisplay = true;

    @Expose
    public Position gardenLevelPos = new Position(390, 40, false, true);

    @Expose
    @ConfigOption(name = "Farming Weight", desc = "")
    @ConfigEditorAccordion(id = 11)
    public boolean eliteFarmingWeight = false;

    @Expose
    @ConfigOption(name = "Display", desc = "Display your farming weight on screen. " +
            "The calculation and API is provided by The Elite SkyBlock farmers. " +
            "See §ehttps://elitebot.dev/info §7for more info.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    @FeatureToggle
    public boolean eliteFarmingWeightDisplay = true;

    @Expose
    public Position eliteFarmingWeightPos = new Position(180, 10, false, true);

    @Expose
    @ConfigOption(name = "Leaderboard Ranking", desc = "Show your position in the farming weight leaderboard. " +
            "Only if your farming weight is high enough! Updates every 10 minutes.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean eliteFarmingWeightLeaderboard = true;

    @Expose
    @ConfigOption(name = "Overtake ETA", desc = "Show a timer estimating when you'll move up a spot in the leaderboard! " +
            "Will show an ETA to rank #10,000 if you're not on the leaderboard yet.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean eliteFarmingWeightOvertakeETA = false;

    @Expose
    @ConfigOption(name = "Offscreen Drop Message", desc = "Show a chat message when joining Garden how many spots you have dropped since last Garden join.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean eliteFarmingWeightOffScreenDropMessage = true;

    @Expose
    @ConfigOption(name = "Always ETA", desc = "Show the Overtake ETA always, even when not farming at the moment.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean eliteFarmingWeightOvertakeETAAlways = true;

    @Expose
    @ConfigOption(name = "ETA Goal", desc = "Override the Overtake ETA to show when you'll reach the specified rank (if not there yet). (Default: \"10,000\")")
    @ConfigEditorText
    @ConfigAccordionId(id = 11)
    public String eliteFarmingWeightETAGoalRank = "10000";

    @Expose
    @ConfigOption(name = "Show below 200", desc = "Show the farming weight data even if you are below 200 weight.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean eliteFarmingWeightIgnoreLow = false;

    @Expose
    @ConfigOption(name = "Show outside garden", desc = "Show the farming weight outside of the garden.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean showWeightOutsideGarden = false;

    @Expose
    @ConfigOption(name = "Show outside SB", desc = "Show the farming weight outside of SkyBlock.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean showWeightOutsideSB = false;

    @Expose
    @ConfigOption(name = "Dicer Counter", desc = "")
    @ConfigEditorAccordion(id = 12)
    public boolean dicerCounter = false;

    @Expose
    @ConfigOption(name = "RNG Drop Counter", desc = "Count RNG drops for Melon Dicer and Pumpkin Dicer.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 12)
    @FeatureToggle
    public boolean dicerCounterDisplay = true;

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat message when dropping a RNG Dicer drop.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 12)
    @FeatureToggle
    public boolean dicerCounterHideChat = false;

    @Expose
    public Position dicerCounterPos = new Position(16, -232, false, true);

    @Expose
    @ConfigOption(name = "Money per Hour", desc = "")
    @ConfigEditorAccordion(id = 13)
    public boolean moneyPerHour = false;

    @Expose
    @ConfigOption(name = "Show Money per Hour",
            desc = "Displays the money per hour YOU get with YOUR crop/minute value when selling the item to bazaar. " +
                    "Supports Bountiful, Mushroom Cow Perk, Armor Crops and Dicer Drops. Their toggles are below.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    @FeatureToggle
    public boolean moneyPerHourDisplay = true;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Only Show Top", desc = "Only show the best # items.")
    @ConfigEditorSlider(
            minValue = 1,
            maxValue = 25,
            minStep = 1
    )
    @ConfigAccordionId(id = 13)
    public int moneyPerHourShowOnlyBest = 5;

    @Expose
    @ConfigOption(name = "Extend Top List", desc = "Add current crop to the list if its lower ranked than the set limit by extending the list.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourShowCurrent = true;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(
            name = "Always On",
            desc = "Always show the money/hour Display while on the garden.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourAlwaysOn = false;

    @Expose
    @ConfigOption(
            name = "Compact Mode",
            desc = "Hide the item name and the position number.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourCompact = false;

    @Expose
    @ConfigOption(
            name = "Compact Price",
            desc = "Show the price more compact.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourCompactPrice = false;

    @Expose
    @ConfigOption(
            name = "Use Custom",
            desc = "Use the custom format below instead of classic ➜ §eSell Offer §7and other profiles ➜ §eNPC Price.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourUseCustomFormat = false;

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
    @ConfigAccordionId(id = 13)
    public List<Integer> moneyPerHourCustomFormat = new ArrayList<>(Arrays.asList(0, 1, 2));

    @Expose
    @ConfigOption(
            name = "Merge Seeds",
            desc = "Merge the seeds price with the wheat price.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourMergeSeeds = true;

    @Expose
    @ConfigOption(
            name = "Include Bountiful",
            desc = "Includes the coins from Bountiful in the calculation.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourBountiful = true;

    @Expose
    @ConfigOption(
            name = "Include Mooshroom Cow",
            desc = "Includes the coins you get from selling the mushrooms from your Mooshroom Cow pet.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourMooshroom = true;

    @Expose
    @ConfigOption(
            name = "Include Armor Drops",
            desc = "Includes the average coins/hr from your armor.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourArmor = true;

    @Expose
    @ConfigOption(
            name = "Include Dicer Drops",
            desc = "Includes the average coins/hr from your melon or pumpkin dicer.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourDicer = true;

    @Expose
    @ConfigOption(
            name = "Hide Title",
            desc = "Hides the first line of 'Money Per Hour' entirely.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourHideTitle = false;

    @Expose
    public Position moneyPerHourPos = new Position(-330, 170, false, true);

    @Expose
    @ConfigOption(name = "Next Jacob's Contest", desc = "")
    @ConfigEditorAccordion(id = 14)
    public boolean nextJacobContest = false;

    @Expose
    @ConfigOption(name = "Show Jacob's Contest", desc = "Show the current or next Jacob's farming contest time and crops.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    @FeatureToggle
    public boolean nextJacobContestDisplay = true;

    @Expose
    @ConfigOption(name = "Show Outside Garden", desc = "Show the timer not only in Garden but everywhere in SkyBlock.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    public boolean nextJacobContestOutsideGarden = false;

    @Expose
    @ConfigOption(name = "Show Outside SB", desc = "Show the timer outside of SkyBlock.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    public boolean nextJacobContestOutsideSB = false;

    @Expose
    @ConfigOption(name = "In Other Guis", desc = "Mark the current or next Farming Contest crops in other farming GUIs as underlined.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    public boolean nextJacobContestOtherGuis = false;

    @Expose
    @ConfigOption(name = "Fetch Contests", desc = "Automatically fetch Contests from elitebot.dev for the current year if they're uploaded already.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    public boolean nextJacobContestsFetchAutomatically = true;

    @Expose
    @ConfigOption(name = "Share Contests", desc = "Share the list of upcoming Contests to elitebot.dev for everyone else to then fetch automatically.")
    @ConfigEditorDropdown(values = { "Ask When Needed", "Share Automatically", "Disabled" })
    @ConfigAccordionId(id = 14)
    public int nextJacobContestsShareAutomatically = 0;

    @Expose
    @ConfigOption(name = "Warning", desc = "Show a warning shortly before a new Jacob's Contest starts.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    public boolean nextJacobContestWarn = false;

    @Expose
    @ConfigOption(name = "Warning Time", desc = "Set the warning time in seconds before a Jacob's Contest begins.")
    @ConfigEditorSlider(
            minValue = 10,
            maxValue = 60 * 5,
            minStep = 1
    )
    @ConfigAccordionId(id = 14)
    public int nextJacobContestWarnTime = 60 * 2;

    @Expose
    @ConfigOption(name = "Popup Warning", desc = "Opens a popup when the warning time is reached and Minecraft is not in focus.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    public boolean nextJacobContestWarnPopup = false;

    @Expose
    public Position nextJacobContestPos = new Position(-200, 10, false, true);

    @Expose
    @ConfigOption(name = "Farming Armor Drops", desc = "")

    @ConfigEditorAccordion(id = 18)
    public boolean farmingArmorDrops = false;

    @Expose
    @ConfigOption(name = "Show Counter", desc = "Count all §9Cropie§7, §5Squash §7and §6Fermento §7dropped.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 18)
    @FeatureToggle
    public boolean farmingArmorDropsEnabled = true;

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat message when receiving a farming armor drop.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 18)
    @FeatureToggle
    public boolean farmingArmorDropsHideChat = false;

    @Expose
    public Position farmingArmorDropsPos = new Position(16, -232, false, true);

    @Expose
    @ConfigOption(name = "Anita Shop", desc = "")
    @ConfigEditorAccordion(id = 16)
    public boolean anitaMedalProfit = false;

    @Expose
    @ConfigOption(
            name = "Medal Prices",
            desc = "Helps to identify profitable items to buy at the Anita item shop " +
                    "and potential profit from selling the item in the Auction House."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 16)
    @FeatureToggle
    public boolean anitaMedalProfitEnabled = true;

    @Expose
    @ConfigOption(
            name = "Extra Farming Fortune",
            desc = "Show current tier and cost to max out in the item tooltip.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 16)
    @FeatureToggle
    public boolean extraFarmingFortune = true;

    @Expose
    public Position anitaMedalProfitPos = new Position(206, 158, false, true);

    @Expose
    @ConfigOption(name = "Composter", desc = "")
    @ConfigEditorAccordion(id = 17)
    public boolean composter = false;

    @Expose
    @ConfigOption(
            name = "Composter Overlay",
            desc = "Show organic matter, fuel, and profit prices while inside the Composter Inventory."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    @FeatureToggle
    public boolean composterOverlay = true;

    @Expose
    @ConfigOption(name = "Overlay Price", desc = "Toggle for Bazaar 'buy order' vs 'instant buy' price in composter overlay.")
    @ConfigEditorDropdown(values = {"Instant Buy", "Buy Order"})
    @ConfigAccordionId(id = 17)
    public int composterOverlayPriceType = 0;

    @Expose
    @ConfigOption(name = "Retrieve From", desc = "Change where to retrieve the materials from in the composter overlay: The Bazaar or Sacks.")
    @ConfigEditorDropdown(values = {"Bazaar", "Sacks"})
    @ConfigAccordionId(id = 17)
    public int composterOverlayRetrieveFrom = 0;

    @Expose
    public Position composterOverlayOrganicMatterPos = new Position(140, 152, false, true);

    @Expose
    public Position composterOverlayFuelExtrasPos = new Position(-320, 152, false, true);

    @Expose
    @ConfigOption(
            name = "Display Element",
            desc = "Displays the Compost data from the tab list as GUI element."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    @FeatureToggle
    public boolean composterDisplayEnabled = true;

    @Expose
    @ConfigOption(
            name = "Outside Garden",
            desc = "Show Time till Composter is empty outside Garden"
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    @FeatureToggle
    public boolean composterDisplayOutsideGarden = false;

    @Expose
    @ConfigOption(
            name = "Composter Warning",
            desc = "Warn when the Composter gets close to empty, even outside Garden."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    public boolean composterWarnAlmostClose = false;

    @Expose
    @ConfigOption(
            name = "Upgrade Price",
            desc = "Show the price for the Composter Upgrade in the lore."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    @FeatureToggle
    public boolean composterUpgradePrice = true;

    @Expose
    @ConfigOption(
            name = "Round Amount Needed",
            desc = "Rounds the amount needed to fill your Composter down so that you don't overspend."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    public boolean composterRoundDown = true;

    @Expose
    @ConfigOption(
            name = "Highlight Upgrade",
            desc = "Highlight Upgrades that can be bought right now."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    @FeatureToggle
    public boolean composterHighLightUpgrade = true;

    @Expose
    @ConfigOption(
            name = "Inventory Numbers",
            desc = "Show the amount of Organic Matter, Fuel and Composts Available while inside the Composter Inventory."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    @FeatureToggle
    public boolean composterInventoryNumbers = true;

    @Expose
    @ConfigOption(name = "Notification When Low Composter", desc = "")
    @ConfigAccordionId(id = 17)
    @ConfigEditorAccordion(id = 21)
    public boolean composterNotifyLow = false;

    @Expose
    @ConfigOption(name = "Enable", desc = "Show a notification when Organic Matter or Fuel runs low in your Composter.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 21)
    @FeatureToggle
    public boolean composterNotifyLowEnabled = false;

    @Expose
    @ConfigOption(name = "Show Title", desc = "Send a title to notify.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 21)
    public boolean composterNotifyLowTitle = false;

    @Expose
    @ConfigOption(name = "Min Organic Matter", desc = "Warn when Organic Matter is below this value.")
    @ConfigEditorSlider(
            minValue = 1_000,
            maxValue = 80_000,
            minStep = 1
    )
    @ConfigAccordionId(id = 21)
    public int composterNotifyLowOrganicMatter = 20_000;

    @Expose
    @ConfigOption(name = "Min Fuel Cap", desc = "Warn when Fuel is below this value.")
    @ConfigEditorSlider(
            minValue = 500,
            maxValue = 40_000,
            minStep = 1
    )
    @ConfigAccordionId(id = 21)
    public int composterNotifyLowFuel = 10_000;

    @Expose
    public Position composterDisplayPos = new Position(-390, 10, false, true);

    @Expose
    public Position composterOutsideGardenPos = new Position(-363, 13, false, true);

    @Expose
    @ConfigOption(name = "Farming Fortune Display", desc = "")
    @ConfigEditorAccordion(id = 22)
    public boolean farmingFortune = false;

    @Expose
    @ConfigOption(
            name = "FF Display",
            desc = "Displays the true Farming Fortune for the current crop, including all crop-specific and hidden bonuses."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 22)
    @FeatureToggle
    public boolean farmingFortuneDisplay = true;

    @Expose
    @ConfigOption(
            name = "Show As Drop Multiplier",
            desc = "Adds 100 to the displayed Farming Fortune so that it represents a drop multiplier rather than" +
                    " the chance for bonus drops. "
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 22)
    public boolean farmingFortuneDropMultiplier = true;

    @ConfigOption(name = "Farming Fortune Guide", desc = "Opens a guide that breaks down your Farming Fortune.\n§eCommand: /ff")
    @ConfigEditorButton(buttonText = "Open")
    public Runnable open = Commands::openFortuneGuide;

    @Expose
    public Position farmingFortunePos = new Position(5, -180, false, true);

    @Expose
    @ConfigOption(name = "Tooltip Tweaks", desc = "")
    @ConfigEditorAccordion(id = 20)
    public boolean tooltipTweaks = false;

    @Expose
    @ConfigOption(
            name = "Compact Descriptions",
            desc = "Hides redundant parts of reforge descriptions, generic counter description, and Farmhand perk explanation."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 20)
    @FeatureToggle
    public boolean compactToolTooltips = false;

    @Expose
    @ConfigOption(
            name = "Breakdown Hotkey",
            desc = "When the keybind is pressed, show a breakdown of all fortune sources on a tool."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    @ConfigAccordionId(id = 20)
    public int fortuneTooltipKeybind = Keyboard.KEY_LSHIFT;

    @Expose
    @ConfigOption(
            name = "Tooltip Format",
            desc = "Show crop-specific Farming Fortune in tooltip.\n" +
                    "§fShow: §7Crop-specific Fortune indicated as §6[+196]\n" +
                    "§fReplace: §7Edits the total Fortune to include crop-specific Fortune."
    )
    @ConfigEditorDropdown(values = {"Default", "Show", "Replace"})
    @ConfigAccordionId(id = 20)
    public int cropTooltipFortune = 1;

    @Expose
    @ConfigOption(
            name = "Total Crop Milestone",
            desc = "Shows the progress bar till maxed crop milestone in the crop milestone inventory."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 20)
    @FeatureToggle
    public boolean cropMilestoneTotalProgress = true;

    @Expose
    @ConfigOption(name = "Yaw and Pitch", desc = "")
    @Accordion
    public YawPitchDisplay yawPitchDisplay = new YawPitchDisplay();

    public static class YawPitchDisplay {

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
        @ConfigOption(name = "Show Outside Garden", desc = "Show the overlay outside of the Garden.")
        @ConfigEditorBoolean
        public boolean showOutsideGarden = false;

        @Expose
        @ConfigOption(name = "Show Outside SB", desc = "Show the overlay outside of SkyBlock.")
        @ConfigEditorBoolean
        public boolean showOutsideSB = false;

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
    public CropStartLocation cropStartLocation = new CropStartLocation();

    public static class CropStartLocation {

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
