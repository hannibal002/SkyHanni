package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import io.github.moulberry.moulconfig.annotations.*;
import io.github.moulberry.moulconfig.observer.Property;

public class Fishing {

    @ConfigOption(name = "Trophy Fishing", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean trophyFishing = false;

    @ConfigOption(
            name = "Trophy Counter",
            desc = "Counts Trophy messages from chat and tells you how many you have found."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean trophyCounter = false;

    @ConfigOption(
            name = "Trophy Counter Design",
            desc = "§fStyle 1: §72. §6§lGOLD §5Moldfin\n" +
                    "§fStyle 2: §bYou caught a §5Moldfin §6§lGOLD§b. §7(2)\n" +
                    "§fStyle 3: §bYou caught your 2nd §6§lGOLD §5Moldfin§b."
    )
    @ConfigEditorDropdown(values = {"Style 1", "Style 2", "Style 3"})
    @ConfigAccordionId(id = 0)
    public int trophyDesign = 0;

    @ConfigOption(name = "Hide Repeated Catches", desc = "Delete past catches of the same trophy fish from chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean trophyFishDuplicateHider = false;

    @ConfigOption(name = "Bronze Duplicates", desc = "Hide duplicate messages for bronze trophy fishes from chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean trophyFishBronzeHider = false;

    @ConfigOption(name = "Silver Duplicates", desc = "Hide duplicate messages for silver trophy fishes from chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean trophyFishSilverHider = false;

    @ConfigOption(name = "Odger Waypoint", desc = "Show the Odger waypoint when trophy fishes are in the inventory and no lava rod in hand.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean odgerLocation = true;

    @ConfigOption(name = "Thunder Spark", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean thunderSpark = false;

    @ConfigOption(name = "Thunder Spark Highlight", desc = "Highlight Thunder Sparks after killing a Thunder")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean thunderSparkHighlight = false;

    @ConfigOption(name = "Thunder Spark Color", desc = "Color of the Thunder Sparks")
    @ConfigEditorColour
    @ConfigAccordionId(id = 1)
    public String thunderSparkColor = "0:255:255:255:255";

    @ConfigOption(name = "Barn Fishing Timer", desc = "")
    @ConfigEditorAccordion(id = 2)
    public boolean barnTimer_ = false;

    @ConfigOption(
            name = "Barn Fishing Timer",
            desc = "Show the time and amount of sea creatures while fishing on the barn via hub."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean barnTimer = true;

    public Position barnTimerPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Fishing Timer Alert", desc = "Change the amount of time in seconds until the timer dings.")
    @ConfigEditorSlider(
            minValue = 240,
            maxValue = 360,
            minStep = 10
    )
    @ConfigAccordionId(id = 2)
    public int barnTimerAlertTime = 330;

    @ConfigOption(name = "Chum/Chumcap Bucket Hider", desc = "")
    @Accordion
    public ChumBucketHider chumBucketHider = new ChumBucketHider();

    public static class ChumBucketHider {

        @ConfigOption(name = "Enable", desc = "Hide the Chum/Chumcap Bucket name tags for other players.")
        @ConfigEditorBoolean
        public Property<Boolean> enabled = Property.of(true);

        @ConfigOption(name = "Hide Bucket", desc = "Hide the Chum/Chumcap Bucket.")
        @ConfigEditorBoolean
        public Property<Boolean> hideBucket = Property.of(false);

        @ConfigOption(name = "Hide Own", desc = "Hides your own Chum/Chumcap Bucket.")
        @ConfigEditorBoolean
        public Property<Boolean> hideOwn = Property.of(false);
    }

    @ConfigOption(name = "Fished Item Name", desc = "")
    @Accordion
    public FishedItemName fishedItemName = new FishedItemName();

    public static class FishedItemName {

        @ConfigOption(name = "Enabled", desc = "Show the fished item name above the item when fishing.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @ConfigOption(name = "Show Bait", desc = "Also show the name of the consumed bait.")
        @ConfigEditorBoolean
        public boolean showBaits = false;

    }

    @ConfigOption(
            name = "Shark Fish Counter",
            desc = "Counts how many sharks have been caught."
    )
    @ConfigEditorBoolean
    public boolean sharkFishCounter = false;

    public Position sharkFishCounterPos = new Position(10, 10, false, true);

    @ConfigOption(name = "Shorten Fishing Message", desc = "Shortens the chat message that says what type of sea creature you have fished.")
    @ConfigEditorBoolean
    public boolean shortenFishingMessage = false;
}
