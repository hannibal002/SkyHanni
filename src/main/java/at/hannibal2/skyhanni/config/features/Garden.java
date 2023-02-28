package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.*;
import com.google.gson.annotations.Expose;

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
    @ConfigOption(name = "Visitor Timer", desc = "Timer when the next visitor will appear," +
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
    @ConfigOption(name = "Notification", desc = "Show as title and in chat when a new visitor is visiting your island.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean visitorNotification = true;

    @Expose
    @ConfigOption(name = "Highlight", desc = "Highlight visitor when the required items are in the inventory or the visitor is new and needs to checked what items it needs.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean visitorHighlight = true;

    @Expose
    @ConfigOption(name = "Show Price", desc = "Show the bazaar price of the items required for the visitors.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean visitorShowPrice = true;

    @Expose
    @ConfigOption(name = "Numbers", desc = "")
    @ConfigEditorAccordion(id = 4)
    public boolean numbers = false;

    @Expose
    @ConfigOption(name = "Crop Milestone", desc = "Show the number of crop milestones in the inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean numberCropMilestone = true;

    @Expose
    @ConfigOption(name = "Crop Upgrades", desc = "Show the number of upgrades in the crop upgrades inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 4)
    public boolean numberCropUpgrades = true;

    @Expose
    @ConfigOption(name = "Crop Milestone", desc = "")
    @ConfigEditorAccordion(id = 5)
    public boolean cropMilestone = false;

    @Expose
    @ConfigOption(
            name = "Progress Display",
            desc = "Shows the progress and ETA until the next crop milestone is reached and the current crops/minute value. " +
                    "§cRequires a tool with either a counter or cultivating enchantment."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 5)
    public boolean cropMilestoneProgress = true;

    @Expose
    @ConfigOption(name = "Display Position", desc = "")
    @ConfigEditorButton(runnableId = "cropMilestoneProgress", buttonText = "Edit")
    @ConfigAccordionId(id = 5)
    public Position cropMilestoneProgressDisplayPos = new Position(-363, 12, false, true);

    @Expose
    @ConfigOption(name = "Best Crop", desc = "")
    @ConfigAccordionId(id = 5)
    @ConfigEditorAccordion(id = 6)
    public boolean cropMilestoneNext = false;

    @Expose
    @ConfigOption(
            name = "Best Display",
            desc = "Lists all crops and their ETA till next milestone. Sorts for best crop for getting garden level or skyblock level.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 6)
    public boolean cropMilestoneBestDisplay = true;

    @Expose
    @ConfigOption(name = "Sort Type", desc = "Sort the crops by either garden exp or by skyblock exp.")
    @ConfigEditorDropdown(values = {"Garden Exp", "SkyBlock Exp"})
    @ConfigAccordionId(id = 6)
    public int cropMilestoneBestType = 0;

    @Expose
    @ConfigOption(name = "Only show top", desc = "Only show the top # crops.")
    @ConfigEditorSlider(
            minValue = 1,
            maxValue = 10,
            minStep = 1
    )
    @ConfigAccordionId(id = 6)
    public int cropMilestoneShowOnlyBest = 10;

    @Expose
    @ConfigOption(name = "Display Position", desc = "")
    @ConfigEditorButton(runnableId = "cropMilestoneNext", buttonText = "Edit")
    @ConfigAccordionId(id = 6)
    public Position cropMilestoneNextDisplayPos = new Position(-112, -143, false, true);

    @Expose
    @ConfigOption(name = "Plot Price", desc = "Show the price of the plot in coins when inside the Configure Plots inventory.")
    @ConfigEditorBoolean
    public boolean plotPrice = true;
}
