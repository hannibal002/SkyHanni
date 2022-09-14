package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.annotations.*;
import com.google.gson.annotations.Expose;

public class Fishing {

    @ConfigOption(name = "Trophy Fishing", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean trophyFishing = false;

    @Expose
    @ConfigOption(name = "Trophy Counter", desc = "Counts every single Trohy message from chat and tells you how many you got already.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean trophyCounter = false;

    @Expose
    @ConfigOption(name = "Hide Bronze Duplicates", desc = "Hide duplicate messages for bronze trophy fishes from chat.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean trophyFishBronzeHider = false;

    @Expose
    @ConfigOption(name = "Shorten Fishing Message", desc = "Shorten the message what type of sea creature you have fished.")
    @ConfigEditorBoolean
    public boolean shortenFishingMessage = false;

    @ConfigOption(name = "Thunder Spark", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean thunderSpark = false;

    @Expose
    @ConfigOption(name = "Thunder Spark Highlight", desc = "Highlight Thunder Sparks after killing a Thunder")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean thunderSparkHighlight = false;

    @Expose
    @ConfigOption(
            name = "Thunder Spark Color",
            desc = "Color of the Thunder Sparks"
    )
    @ConfigEditorColour
    @ConfigAccordionId(id = 1)
    public String thunderSparkColor = "0:245:FF:FF:FF";
}