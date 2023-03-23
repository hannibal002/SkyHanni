package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.*;
import com.google.gson.annotations.Expose;

public class Fishing {

    @ConfigOption(name = "Trophy Fishing", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean trophyFishing = false;

    @Expose
    @ConfigOption(
            name = "Trophy Counter",
            desc = "Counts Trophy messages from chat and tells you how many you have found."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean trophyCounter = false;

    @Expose
    @ConfigOption(
            name = "Trophy Counter Design",
            desc = "§fStyle 1: §72. §6§lGOLD §5Moldfin\n" +
                    "§fStyle 2: §bYou caught a §5Moldfin §6§lGOLD§b. §7(2)\n" +
                    "§fStyle 3: §bYou caught your 2nd §6§lGOLD §5Moldfin§b."
    )
    @ConfigEditorDropdown(values = {"Style 1", "Style 2", "Style 3"})
    @ConfigAccordionId(id = 0)
    public int trophyDesign = 0;

    @Expose
    @ConfigOption(name = "Hide Repeated Catches", desc = "Delete past catches of the same fish from chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean trophyFishDuplicateHider = false;

    @Expose
    @ConfigOption(name = "Bronze Duplicates", desc = "Hide duplicate messages for bronze trophy fishes from chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean trophyFishBronzeHider = false;

    @Expose
    @ConfigOption(name = "Silver Duplicates", desc = "Hide duplicate messages for silver trophy fishes from chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean trophyFishSilverHider = false;

    @Expose
    @ConfigOption(name = "Odger Waypoint", desc = "Show the Odger waypoint when trophy fishes are in the inventory and no lava rod in hand.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean odgerLocation = true;

    @ConfigOption(name = "Thunder Spark", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean thunderSpark = false;

    @Expose
    @ConfigOption(name = "Thunder Spark Highlight", desc = "Highlight Thunder Sparks after killing a Thunder")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean thunderSparkHighlight = false;

    @Expose
    @ConfigOption(name = "Thunder Spark Color", desc = "Color of the Thunder Sparks")
    @ConfigEditorColour
    @ConfigAccordionId(id = 1)
    public String thunderSparkColor = "0:255:255:255:255";

    @ConfigOption(name = "Barn Fishing Timer", desc = "")
    @ConfigEditorAccordion(id = 2)
    public boolean barnTimer_ = false;

    @Expose
    @ConfigOption(
            name = "Barn Fishing Timer",
            desc = "Show the time and amount of sea creatures while fishing on the barn via hub."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean barnTimer = true;

    @Expose
    @ConfigOption(name = "Fishing Timer Location", desc = "")
    @ConfigEditorButton(runnableId = "barnTimer", buttonText = "Edit")
    @ConfigAccordionId(id = 2)
    public Position barnTimerPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Fishing Timer Alert", desc = "Change the amount of time in seconds until the timer dings.")
    @ConfigEditorSlider(
            minValue = 240,
            maxValue = 360,
            minStep = 10
    )
    @ConfigAccordionId(id = 2)
    public int barnTimerAlertTime = 330;

    @ConfigOption(name = "Shark Fish", desc = "")
    @ConfigEditorAccordion(id = 3)
    public boolean sharkFish = false;

    @Expose
    @ConfigOption(
            name = "Shark Fish Counter",
            desc = "Counts how many sharks have been caught."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 3)
    public boolean sharkFishCounter = false;

    @Expose
    @ConfigOption(name = "Shark Location", desc = "")
    @ConfigEditorButton(runnableId = "sharkFishCounter", buttonText = "Edit")
    @ConfigAccordionId(id = 3)
    public Position sharkFishCounterPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Shorten Fishing Message", desc = "Shortens the chat message that says what type of sea creature you have fished.")
    @ConfigEditorBoolean
    public boolean shortenFishingMessage = false;
}
