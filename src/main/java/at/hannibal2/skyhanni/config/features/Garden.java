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
    @ConfigOption(name = "Copper Price Position", desc = "")
    @ConfigEditorButton(runnableId = "skyMartCopperPrice", buttonText = "Edit")
    @ConfigAccordionId(id = 0)
    public Position skyMartCopperPricePos = new Position(44, -108, false, true);

    @Expose
    @ConfigOption(name = "Visitor Helper", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean visitorHelper = false;

    @Expose
    @ConfigOption(name = "Visitor Timer", desc = "")
    @ConfigAccordionId(id = 1)
    @ConfigEditorAccordion(id = 2)
    public boolean visitorHelperTimer = false;

    @Expose
    @ConfigOption(name = "Visitor Timer", desc = "Timer when the next visitor will appear," +
            "and a number how many visitors are already waiting.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean visitorHelperTimerEnabled = true;

    @Expose
    @ConfigOption(name = "Visitor Timer Position", desc = "")
    @ConfigEditorButton(runnableId = "visitorHelperTimer", buttonText = "Edit")
    @ConfigAccordionId(id = 2)
    public Position visitorHelperTimerPos = new Position(0, 0, false, true);

    @Expose
    @ConfigOption(name = "Visitor Display", desc = "Show all items needed for the visitors.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean visitorHelperDisplay = true;

    @Expose
    @ConfigOption(name = "Visitor Helper Position", desc = "")
    @ConfigEditorButton(runnableId = "visitorHelper", buttonText = "Edit")
    @ConfigAccordionId(id = 1)
    public Position visitorHelperPos = new Position(0, 0, false, true);

    @Expose
    @ConfigOption(name = "Highlight Ready", desc = "Highlight the visitor when the required items are in the inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean visitorHelperHighlightReady = true;

    @Expose
    @ConfigOption(name = "Show Price", desc = "Show the bazaar price of the items required for the visitors.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean visitorHelperShowPrice = true;

    @Expose
    @ConfigOption(name = "Numbers", desc = "")
    @ConfigEditorAccordion(id = 3)
    public boolean numbers = false;

    @Expose
    @ConfigOption(name = "Crop Milestone", desc = "Show the number of the crop milestone in the inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean cropMilestoneNumber = true;

    @Expose
    @ConfigOption(name = "Crop Upgrades", desc = "Show the number of upgrades in the crop upgrades inventory.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean cropUpgradesNumber = true;
}
