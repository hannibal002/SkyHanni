package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.*;
import com.google.gson.annotations.Expose;
import org.lwjgl.input.Keyboard;

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
    @ConfigOption(name = "Copper Price Position", desc = "")
    @ConfigEditorButton(runnableId = "skyMartCopperPrice", buttonText = "Edit")
    @ConfigAccordionId(id = 0)
    public Position skyMartCopperPricePos = new Position(188, -105, false, true);

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
            "and a number how many visitors are already waiting.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean visitorTimerEnabled = true;

    @Expose
    @ConfigOption(name = "Visitor Timer Position", desc = "")
    @ConfigEditorButton(runnableId = "visitorTimer", buttonText = "Edit")
    @ConfigAccordionId(id = 2)
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
    @ConfigOption(name = "Items Needed Position", desc = "")
    @ConfigEditorButton(runnableId = "visitorNeeds", buttonText = "Edit")
    @ConfigAccordionId(id = 3)
    public Position visitorNeedsPos = new Position(155, -57, false, true);

    @Expose
    @ConfigOption(name = "Only when Close", desc = "Only show the needed items when close to the visitors.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean visitorNeedsOnlyWhenClose = false;

    @Expose
    @ConfigOption(name = "Visitor Inventory", desc = "")
    @ConfigAccordionId(id = 1)
    @ConfigEditorAccordion(id = 4)
    public boolean visitorInventory = false;

    @Expose
    @ConfigOption(name = "Visitor Price", desc = "Show the bazaar price of the items required for the visitors. (Like in NEU)")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean visitorShowPrice = false;

    @Expose
    @ConfigOption(name = "Amount and Time", desc = "Show the exact item amount and the remaining time when farmed manually. Especially useful for ironman.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean visitorExactAmountAndTime = true;

    @Expose
    @ConfigOption(name = "Copper Price", desc = "Show the price for copper inside the visitor gui.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean visitorCopperPrice = false;

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
    @ConfigOption(name = "Highlight", desc = "Highlight visitor when the required items are in the inventory or the visitor is new and needs to checked what items it needs.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean visitorHighlight = true;

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
    @ConfigOption(name = "Display Position", desc = "")
    @ConfigEditorButton(runnableId = "cropMilestoneProgress", buttonText = "Edit")
    @ConfigAccordionId(id = 6)
    public Position cropMilestoneProgressDisplayPos = new Position(376, 19, false, true);

    @Expose
    @ConfigOption(name = "Best Crop", desc = "")
    @ConfigAccordionId(id = 6)
    @ConfigEditorAccordion(id = 7)
    public boolean cropMilestoneNext = false;
    // TODO moulconfig runnable suppoort

    @Expose
    @ConfigOption(
            name = "Best Display",
            desc = "Lists all crops and their ETA till next milestone. Sorts for best crop for getting garden level or skyblock level.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    public boolean cropMilestoneBestDisplay = true;

    // TODO moulconfig runnable suppoort
    @Expose
    @ConfigOption(name = "Sort Type", desc = "Sort the crops by either garden exp or by skyblock exp.")
    @ConfigEditorDropdown(values = {"Garden Exp", "SkyBlock Exp"})
    @ConfigAccordionId(id = 7)
    public int cropMilestoneBestType = 0;

    // TODO moulconfig runnable suppoort
    @Expose
    @ConfigOption(name = "Only show top", desc = "Only show the top # crops.")
    @ConfigEditorSlider(
            minValue = 1,
            maxValue = 10,
            minStep = 1
    )
    @ConfigAccordionId(id = 7)
    public int cropMilestoneShowOnlyBest = 10;

    // TODO moulconfig runnable suppoort
    @Expose
    @ConfigOption(
            name = "Always On",
            desc = "Show the Best Display always while on the garden.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 7)
    public boolean cropMilestoneBestAlwaysOn = false;

    @Expose
    @ConfigOption(name = "Display Position", desc = "")
    @ConfigEditorButton(runnableId = "cropMilestoneNext", buttonText = "Edit")
    @ConfigAccordionId(id = 7)
    public Position cropMilestoneNextDisplayPos = new Position(-112, -143, false, true);

    // TODO moulconfig runnable suppoort
    @Expose
    @ConfigOption(name = "Custom Keybind", desc = "")
    @ConfigEditorAccordion(id = 8)
    public boolean keybind = false;

    @Expose
    @ConfigOption(name = "Enabled", desc = "Use custom keybinds while having a farming tool or Daedalus Axe in the hand in the garden. §cOnly updates after scrolling in the hotbar.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 8)
    public boolean keyBindEnabled = false;

    @ConfigOption(name = "Disable All", desc = "Disabling all keys.")
    @ConfigEditorButton(runnableId = "gardenKeyBindPresetDisabled", buttonText = "Disable")
    @ConfigAccordionId(id = 8)
    public int keyBindPresetDisable = 0;

    @ConfigOption(name = "Set Default", desc = "Reset all keys to default.")
    @ConfigEditorButton(runnableId = "gardenKeyBindPresetDefault", buttonText = "Default")
    @ConfigAccordionId(id = 8)
    public int keyBindPresetDefault = 0;

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
    @ConfigOption(name = "Enabled", desc = "Show the optimal speed for your current tool in the hand. (Ty MelonKingDE for the values)")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean optimalSpeedEnabled = true;

    @Expose
    @ConfigOption(name = "Warning Title", desc = "Warn via title when you don't have the optimal speed.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 9)
    public boolean optimalSpeedWarning = false;

    @Expose
    @ConfigOption(name = "Speed Warning Position", desc = "")
    @ConfigEditorButton(runnableId = "optimalSpeed", buttonText = "Edit")
    @ConfigAccordionId(id = 9)
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
    @ConfigOption(name = "Garden Level Position", desc = "")
    @ConfigEditorButton(runnableId = "gardenLevel", buttonText = "Edit")
    @ConfigAccordionId(id = 10)
    public Position gardenLevelPos = new Position(-375, -215, false, true);

    @Expose
    @ConfigOption(name = "Elite Farming Weight", desc = "")
    @ConfigEditorAccordion(id = 10)
    public boolean eliteFarmingWeight = false;

    @Expose
    @ConfigOption(name = "Display", desc = "Display your farming weight on screen. " +
            "The calculation and api is provided by The Elite SkyBlock Farmers. " +
            "See §ehttps://elitebot.dev/info §7for more info.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 10)
    public boolean eliteFarmingWeightDisplay = true;

    @Expose
    @ConfigOption(name = "Farming Weight Position", desc = "")
    @ConfigEditorButton(runnableId = "eliteFarmingWeight", buttonText = "Edit")
    @ConfigAccordionId(id = 10)
    public Position eliteFarmingWeightPos = new Position(-370, -167, false, true);

    @Expose
    @ConfigOption(name = "Leaderboard Ranking", desc = "Show your position in the farming weight leaderboard. " +
            "Only if your farming weight is high enough! Updates every 10 minutes.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 10)
    public boolean eliteFarmingWeightLeaderboard = true;

    @Expose
    @ConfigOption(name = "Plot Price", desc = "Show the price of the plot in coins when inside the Configure Plots inventory.")
    @ConfigEditorBoolean
    public boolean plotPrice = true;

    @Expose
    @ConfigOption(name = "Desk in Menu", desc = "Show a Desk button in the SkyBlock Menu. Opens the /desk command on click.")
    @ConfigEditorBoolean
    public boolean deskInSkyBlockMenu = true;
}
