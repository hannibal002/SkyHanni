package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Garden {

    @Expose
    @ConfigOption(name = "Sky Mart", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean skyMart = false;

    @Expose
    @ConfigOption(name = "Copper Price", desc = "Show copper to coin prices inside the Sky Mart inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean skyMartCopperPrice = true;

    @Expose
    @ConfigOption(name = "Advanced stats", desc = "Show additionally the bin price and copper price for every item.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean skyMartCopperPriceAdvancedStats = false;

    @Expose
//    @ConfigOption(name = "Copper Price Position", desc = "")
//    @ConfigEditorButton(runnableId = "skyMartCopperPrice", buttonText = "Edit")
//    @ConfigAccordionId(id = 0)
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
    public boolean visitorTimerEnabled = true;

    @Expose
    @ConfigOption(name = "Sixth Visitor Estimate", desc = "Estimate when the sixth visitor in the queue will arrive. " +
            "May be inaccurate with coop members farming simultaneously.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean visitorTimerSixthVisitorEnabled = true;

    @Expose
//    @ConfigOption(name = "Visitor Timer Position", desc = "")
//    @ConfigEditorButton(runnableId = "visitorTimer", buttonText = "Edit")
//    @ConfigAccordionId(id = 2)
    public Position visitorTimerPos = new Position(-373, -203, false, true);

    @Expose
    @ConfigOption(name = "Visitor Items Needed", desc = "")
    @ConfigAccordionId(id = 1)
    @ConfigEditorAccordion(id = 3)
    public boolean visitorNeeds = false;

    @Expose
    @ConfigOption(name = "Items Needed", desc = "Show all items needed for the visitors.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean visitorNeedsDisplay = true;

    @Expose
    public Position visitorNeedsPos = new Position(155, -57, false, true);

    @Expose
    @ConfigOption(name = "Only when Close", desc = "Only show the needed items when close to the visitors.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean visitorNeedsOnlyWhenClose = false;

    @Expose
    @ConfigOption(name = "Bazaar Alley", desc = "Show the Visitor Items List while inside the Bazaar Alley in the Hub. " +
            "This helps buying the correct amount when not having a booster cookie buff active.")
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
    public boolean visitorItemPreview = true;

    @Expose
    @ConfigOption(name = "Visitor Inventory", desc = "")
    @ConfigAccordionId(id = 1)
    @ConfigEditorAccordion(id = 4)
    public boolean visitorInventory = false;

    @Expose
    @ConfigOption(name = "Visitor Price", desc = "Show the bazaar price of the items required for the visitors, like in NEU.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean visitorShowPrice = false;

    @Expose
    @ConfigOption(name = "Amount and Time", desc = "Show the exact item amount and the remaining time when farmed manually. Especially useful for ironman.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean visitorExactAmountAndTime = true;

    @Expose
    @ConfigOption(name = "Copper Price", desc = "Show the price per copper inside the visitor gui.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean visitorCopperPrice = true;

    @Expose
    @ConfigOption(name = "Garden Exp Price", desc = "Show the price per garden experience inside the visitor gui.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean visitorExperiencePrice = false;

    @Expose
    @ConfigOption(name = "Notification Chat", desc = "Show in chat when a new visitor is visiting your island.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean visitorNotificationChat = true;

    @Expose
    @ConfigOption(name = "Notification Title", desc = "Show a title when a new visitor is visiting your island.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
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
    public boolean visitorColoredName = true;

    @Expose
    @ConfigOption(name = "Hypixel Message", desc = "Hide the chat message from hypixel that a new visitor has arrived at your garden")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean visitorHypixelArrivedMessage = true;

    @Expose
    @ConfigOption(name = "Numbers", desc = "")
    @ConfigEditorAccordion(id = 5)
    public boolean numbers = false;

    @Expose
    @ConfigOption(name = "Crop Milestone", desc = "Show the number of crop milestones in the inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    public boolean numberCropMilestone = true;

    @Expose
    @ConfigOption(name = "Crop Upgrades", desc = "Show the number of upgrades in the crop upgrades inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    public boolean numberCropUpgrades = true;

    @Expose
    @ConfigOption(name = "Composter Upgrades", desc = "Show the number of upgrades in the composter upgrades inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    public boolean numberComposterUpgrades = true;

    @Expose
    @ConfigOption(name = "Crop Milestones", desc = "")
    @ConfigEditorAccordion(id = 6)
    public boolean cropMilestones = false;

    @Expose
    @ConfigOption(
            name = "Progress Display",
            desc = "Shows the progress and ETA until the next crop milestone is reached and the current crops/minute value. " +
                    "§cRequires a tool with either a counter or cultivating enchantment."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean cropMilestoneProgress = true;

    @Expose
    @ConfigOption(
            name = "Warn When Close",
            desc = "Warn with title and sound when the next crop milestone upgrade happens in 5 seconds. " +
                    "Useful for switching to a different pet for leveling.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean cropMilestoneWarnClose = true;

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
                    "§7Blocks/Second§8: §e20",
                    "§7Percentage: §e12.34%",
            }
    )
    @ConfigAccordionId(id = 6)
    public List<Integer> cropMilestoneText = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));

    @Expose
    public Position cropMilestoneProgressDisplayPos = new Position(376, 19, false, true);

    @Expose
    @ConfigOption(name = "Best Crop", desc = "")
    @ConfigAccordionId(id = 6)
    @ConfigEditorAccordion(id = 7)
    public boolean cropMilestoneNext = false;
    // TODO moulconfig runnable support

    @Expose
    @ConfigOption(
            name = "Best Display",
            desc = "Lists all crops and their ETA till next milestone. Sorts for best crop for getting garden level or skyblock level.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    public boolean cropMilestoneBestDisplay = true;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Sort Type", desc = "Sort the crops by either garden exp or by skyblock exp.")
    @ConfigEditorDropdown(values = {"Garden Exp", "SkyBlock Exp"})
    @ConfigAccordionId(id = 7)
    public int cropMilestoneBestType = 0;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Only show top", desc = "Only show the top # crops.")
    @ConfigEditorSlider(
            minValue = 1,
            maxValue = 10,
            minStep = 1
    )
    @ConfigAccordionId(id = 7)
    public int cropMilestoneShowOnlyBest = 10;

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
            desc = "Make the best crop time more compact by removing the crop name and exp and making the time format shorter")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    public boolean cropMilestoneBestCompact = false;

    @Expose
    public Position cropMilestoneNextDisplayPos = new Position(-112, -143, false, true);

    @Expose
    @ConfigOption(name = "Mushroom Pet Perk", desc = "")
    @ConfigAccordionId(id = 6)
    @ConfigEditorAccordion(id = 15)
    public boolean cropMilestoneMushroomPetPerk = false;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(
            name = "Display Enabled",
            desc = "Show the progress and ETA for mushroom crops when farming other crops because of the mushroom cow perk.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 15)
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
    @ConfigOption(name = "Enabled", desc = "Use custom keybinds while holding a farming tool or daedalus axe in the  §cOnly updates after scrolling in the hotbar.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 8)
    public boolean keyBindEnabled = false;

    @ConfigOption(name = "Disable All", desc = "Disable all keys.")
    @ConfigEditorButton(buttonText = "Disable")
    @ConfigAccordionId(id = 8)
    public Runnable keyBindPresetDisable = () -> {
        keyBindAttack = Keyboard.KEY_NONE;
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
    @ConfigOption(name = "Enabled", desc = "Show the optimal speed for your current tool in the hand.\n(Thanks MelonKingDE for the default values).")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean optimalSpeedEnabled = true;

    @Expose
    @ConfigOption(name = "Warning Title", desc = "Warn via title when you don't have the optimal speed.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean optimalSpeedWarning = false;

    @Expose
    @ConfigOption(name = "Rancher Boots", desc = "Allows you to set the optimal speed in the rancher boots overlay by clicking on the presets.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean optimalSpeedSignEnabled = true;

    @Expose
    public Position optimalSpeedSignPosition = new Position(-450, 119, false, true);

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
                "§eRacing Helmet§7: §f✦ 500 speed")
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
    public Position optimalSpeedPos = new Position(188, -105, false, true);

    @Expose
    @ConfigOption(name = "Garden Level", desc = "")
    @ConfigEditorAccordion(id = 10)
    public boolean gardenLevel = false;

    @Expose
    @ConfigOption(name = "Display", desc = "Show the current garden level and progress to the next level.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 10)
    public boolean gardenLevelDisplay = true;

    @Expose
    public Position gardenLevelPos = new Position(-375, -215, false, true);

    @Expose
    @ConfigOption(name = "Elite Farming Weight", desc = "")
    @ConfigEditorAccordion(id = 11)
    public boolean eliteFarmingWeight = false;

    @Expose
    @ConfigOption(name = "Display", desc = "Display your farming weight on screen. " +
            "The calculation and api is provided by The Elite SkyBlock Farmers. " +
            "See §ehttps://elitebot.dev/info §7for more info.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean eliteFarmingWeightDisplay = true;

    @Expose
    public Position eliteFarmingWeightPos = new Position(-370, -167, false, true);

    @Expose
    @ConfigOption(name = "Leaderboard Ranking", desc = "Show your position in the farming weight leaderboard. " +
            "Only if your farming weight is high enough! Updates every 10 minutes.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean eliteFarmingWeightLeaderboard = true;

    @Expose
    @ConfigOption(name = "Overtake ETA", desc = "Show a timer estimating when you'll move up a spot in the leaderboard! " +
            "Will show an ETA to rank #1000 if you're not on the leaderboard yet.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean eliteFarmingWeightOvertakeETA = false;

    @Expose
    @ConfigOption(name = "Always ETA", desc = "Show the Overtake ETA always, even when not farming at the moment.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 11)
    public boolean eliteFarmingWeightOvertakeETAAlways = true;

    @Expose
    @ConfigOption(name = "Dicer Counter", desc = "")
    @ConfigEditorAccordion(id = 12)
    public boolean dicerCounter = false;

    @Expose
    @ConfigOption(name = "Rng Drop Counter", desc = "Count RNG drops for Melon Dicer and Pumpkin Dicer.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 12)
    public boolean dicerCounterDisplay = true;

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat message when dropping a RNG Dicer drop.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 12)
    public boolean dicerCounterHideChat = false;

    @Expose
    public Position dicerCounterPos = new Position(16, -232, false, true);

    @Expose
    @ConfigOption(name = "Money per Hour", desc = "")
    @ConfigEditorAccordion(id = 13)
    public boolean moneyPerHour = false;

    @Expose
    @ConfigOption(name = "Show money per Hour",
            desc = "Displays the money per hour YOU get with YOUR crop/minute value when selling the item to bazaar. " +
                    "Supports Bountiful and Mushroom Cow Perk.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourDisplay = true;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(name = "Only show top", desc = "Only show the best # items.")
    @ConfigEditorSlider(
            minValue = 1,
            maxValue = 25,
            minStep = 1
    )
    @ConfigAccordionId(id = 13)
    public int moneyPerHourShowOnlyBest = 5;

    // TODO moulconfig runnable support
    @Expose
    @ConfigOption(
            name = "Always On",
            desc = "Show the money/hour Display always while on the garden.")
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
            desc = "Use the custom format below instead of classic -> §eSell Offer §7and other profiles -> §eNPC Price.")
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
            name = "Hide Title",
            desc = "Hides the first line of 'Money Per Hour' entirely.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 13)
    public boolean moneyPerHourHideTitle = false;

    @Expose
    public Position moneyPerHourPos = new Position(16, -232, false, true);

    @Expose
    @ConfigOption(name = "Next Jacob's Contest", desc = "")
    @ConfigEditorAccordion(id = 14)
    public boolean nextJacobContest = false;

    @Expose
    @ConfigOption(name = "Show Jacob's Contest", desc = "Show the current or next Jacob's farming contest time and crops.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    public boolean nextJacobContestDisplay = true;

    @Expose
    @ConfigOption(name = "Outside Garden", desc = "Show the timer not only in garden but everywhere in skyblock.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    public boolean nextJacobContestEverywhere = false;

    @Expose
    @ConfigOption(name = "In Other Guis", desc = "Mark the current or next farming contest crops in other farming guis as underlined.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    public boolean nextJacobContestOtherGuis = false;

    @Expose
    @ConfigOption(name = "Warning", desc = "Show a warning shortly before a new Jacob's contest starts.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 14)
    public boolean nextJacobContestWarn = false;

    @Expose
    @ConfigOption(name = "Warning Time", desc = "Set the warning time in seconds before a Jacob's contest begins.")
    @ConfigEditorSlider(
            minValue = 10,
            maxValue = 60 * 5,
            minStep = 1
    )
    @ConfigAccordionId(id = 14)
    public int nextJacobContestWarnTime = 60 * 2;

    @Expose
    public Position nextJacobContestPos = new Position(-278, 11, false, true);

    @Expose
    @ConfigOption(name = "Farming Armor Drops", desc = "")

    @ConfigEditorAccordion(id = 18)
    public boolean farmingArmorDrops = false;

    @Expose
    @ConfigOption(name = "Show Counter", desc = "Count all §9Cropie§7, §5Squash §7and §6Fermento §7dropped.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 18)
    public boolean farmingArmorDropsEnabled = true;

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat message when receiving a farming armor drop.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 18)
    public boolean farmingArmorDropsHideChat = false;

    @Expose
    public Position farmingArmorDropsPos = new Position(16, -232, false, true);

    @Expose
    @ConfigOption(name = "Teleport Pads", desc = "")
    @ConfigEditorAccordion(id = 19)
    public boolean teleportPads = false;

    @Expose
    @ConfigOption(name = "Compact Name", desc = "Hide the 'Warp to' and 'No Destination' texts over teleport pads.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 19)
    public boolean teleportPadsCompactName = false;

    @Expose
    @ConfigOption(name = "Inventory Numbers", desc = "Show the number of the teleport pads inside the 'Change Destination' inventory as stack size.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 19)
    public boolean teleportPadsInventoryNumbers = false;

    @Expose
    @ConfigOption(name = "Anita Medal Profit", desc = "")
    @ConfigEditorAccordion(id = 16)
    public boolean anitaMedalProfit = false;

    @Expose
    @ConfigOption(
            name = "Show Prices",
            desc = "Helps to identify profitable items to buy at the Anita item shop " +
                    "and potential profit from selling the item at the auction house."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 16)
    public boolean anitaMedalProfitEnabled = true;

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
    public boolean composterOverlay = true;

    @Expose
    @ConfigOption(name = "Overlay Price", desc = "Toggle for bazaar 'buy order' vs 'instant buy' price in composter overlay.")
    @ConfigEditorDropdown(values = {"Instant Buy", "Buy Order"})
    @ConfigAccordionId(id = 17)
    public int composterOverlayPriceType = 0;

    @Expose
    public Position composterOverlayOrganicMatterPos = new Position(140, 152, false, true);

    @Expose
    public Position composterOverlayFuelExtrasPos = new Position(-320, 152, false, true);

    @Expose
    @ConfigOption(
            name = "Display Element",
            desc = "Displays the compost data from the tab list as gui element."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    public boolean composterDisplayEnabled = true;

    @Expose
    @ConfigOption(
            name = "Upgrade Price",
            desc = "Show the price for the composter upgrade in the lore."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    public boolean composterUpgradePrice = true;

    @Expose
    @ConfigOption(
            name = "Highlight Upgrade",
            desc = "Highlight Upgrades that can be bought right now."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    public boolean composterHighLightUpgrade = true;

    @Expose
    @ConfigOption(
            name = "Inventory Numbers",
            desc = "Show the amount of Organic Matter, Fuel and Composts Available while inside the composter inventory."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 17)
    public boolean composterInventoryNumbers = true;

    @Expose
    @ConfigOption(name = "Notification When Low Composter", desc = "")
    @ConfigAccordionId(id = 17)
    @ConfigEditorAccordion(id = 21)
    public boolean composterNotifyLow = false;

    @Expose
    @ConfigOption(name = "Enable", desc = "Show a notification when organic matter or fuel runs low in your composter.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 21)
    public boolean composterNotifyLowEnabled = true;

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
    public Position composterDisplayPos = new Position(-363, 13, false, true);

    @Expose
    @ConfigOption(name = "True Farming Fortune", desc = "")
    @ConfigEditorAccordion(id = 22)
    public boolean farmingFortune = false;

    @Expose
    @ConfigOption(
            name = "FF Display",
            desc = "Displays current farming fortune, including crop-specific bonuses."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 22)
    public boolean farmingFortuneDisplay = true;

    @Expose
    @ConfigOption(
            name = "Show As Drop Multiplier",
            desc = "Adds 100 to the displayed farming fortune so that it represents a drop multiplier rather than" +
                    " the chance for bonus drops. "
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 22)
    public boolean farmingFortuneDropMultiplier = false;

    @Expose
    public Position farmingFortunePos = new Position(-375, -200, false, true);

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
            desc = "Show crop-specific farming fortune in tooltip.\n" +
                    "§fShow: §7Crop-specific fortune indicated as §6[+196]\n" +
                    "§fReplace: §7Edits the total fortune to include crop-specific fortune."
    )
    @ConfigEditorDropdown(values = {"Default", "Show", "Replace"})
    @ConfigAccordionId(id = 20)
    public int cropTooltipFortune = 1;

    @Expose
    @ConfigOption(name = "Plot Price", desc = "Show the price of the plot in coins when inside the Configure Plots inventory.")
    @ConfigEditorBoolean
    public boolean plotPrice = true;

    @Expose
    @ConfigOption(name = "Desk in Menu", desc = "Show a Desk button in the SkyBlock Menu. Opens the /desk command on click.")
    @ConfigEditorBoolean
    public boolean deskInSkyBlockMenu = true;


    @Expose
    @ConfigOption(name = "Fungi Cutter Warning", desc = "Warn when breaking mushroom with the wrong Fungi Cutter mode.")
    @ConfigEditorBoolean
    public boolean fungiCutterWarn = true;

    @Expose
    @ConfigOption(name = "Always Finnegan", desc = "Forcefully set the Finnegan Farming Simulator perk to be active. This is useful if the auto mayor detection fails.")
    @ConfigEditorBoolean
    public boolean forcefullyEnabledAlwaysFinnegan = false;

    @Expose
    public Position cropSpeedMeterPos = new Position(278, -236, false, true);
}
