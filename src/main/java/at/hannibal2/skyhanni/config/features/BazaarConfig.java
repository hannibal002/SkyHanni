package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class BazaarConfig {

    @Expose
    @ConfigOption(name = "Purchase Helper", desc = "Highlights the item you are trying to buy in the Bazaar.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean purchaseHelper = true;

    @Expose
    @ConfigOption(name = "Order Helper", desc = "Show visual hints inside the Bazaar Manage Order view when items are ready to pickup or outbid.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean orderHelper = false;

    @Expose
    @ConfigOption(name = "Best Sell Method", desc = "Show the price difference between sell instantly and sell offer.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean bestSellMethod = false;

    @Expose
    public Position bestSellMethodPos = new Position(394, 142, false, true);

    @Expose
    @ConfigOption(name = "Cancelled Buy Order Clipboard", desc = "Saves missing items from cancelled buy orders to clipboard for faster re-entry.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean cancelledBuyOrderClipboard = false;

    @Expose
    @ConfigOption(name = "Price Website", desc = "Adds a button to the bazaar product inventory that will open the item page in §cskyblock.bz§7.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean openPriceWebsite = false;
}
