package at.hannibal2.skyhanni.config.features.slayer;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class ItemProfitTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Count all items you pick up while doing slayer, " +
        "keep track of how much you pay for starting slayers and calculating the overall profit.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    public Position pos = new Position(20, 20, false, true);

    @Expose
    @ConfigOption(name = "Price in Chat", desc = "Show an extra chat message when you pick up an item. " +
        "(This contains name, amount and price)")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean priceInChat = false;

    @Expose
    @ConfigOption(name = "Minimum Price", desc = "Items below this price will not show up in chat.")
    @ConfigEditorSlider(minValue = 1, maxValue = 5_000_000, minStep = 1)
    public int minimumPrice = 100_000;

    @Expose
    @ConfigOption(name = "Title Warning", desc = "Show a title for expensive item pickups.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean titleWarning = false;

    @Expose
    @ConfigOption(name = "Title Price", desc = "Items above this price will show up as a title.")
    @ConfigEditorSlider(minValue = 1, maxValue = 20_000_000, minStep = 1)
    public int minimumPriceWarning = 500_000;
}
