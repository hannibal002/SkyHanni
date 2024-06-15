package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.utils.LorenzColor;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class AuctionHousePriceComparisonConfig {

    @Expose
    @ConfigOption(
        name = "Show Price Comparison",
        desc = "Highlight auctions based on the difference between their estimated value and the value they are listed for.\n" +
            "§eThis may be very inaccurate at times and only provides an estimate."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Good Colour", desc = "What colour to highlight good value items with.")
    @ConfigEditorColour
    public String good = LorenzColor.GREEN.toConfigColour();

    @Expose
    @ConfigOption(name = "Very Good Colour", desc = "What colour to highlight very good value items with.")
    @ConfigEditorColour
    public String veryGood = "0:255:0:139:0";

    @Expose
    @ConfigOption(name = "Bad Colour", desc = "What colour to highlight bad items with.")
    @ConfigEditorColour
    public String bad = LorenzColor.YELLOW.toConfigColour();

    @Expose
    @ConfigOption(name = "Very Bad Colour", desc = "What colour to highlight very bad items with.")
    @ConfigEditorColour
    public String veryBad = "0:255:225:43:30";
}
