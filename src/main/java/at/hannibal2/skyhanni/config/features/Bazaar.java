package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class Bazaar {

    @ConfigOption(name = "Purchase Helper", desc = "Highlights the item you are trying to buy in the Bazaar.")
    @ConfigEditorBoolean
    public boolean purchaseHelper = true;

    @ConfigOption(name = "Order Helper", desc = "Show visual hints inside the Bazaar Manage Order view when items are ready to pickup or outbid.")
    @ConfigEditorBoolean
    public boolean orderHelper = false;

    @ConfigOption(name = "Best Sell Method", desc = "Show the price difference between sell instantly and sell offer.")
    @ConfigEditorBoolean
    public boolean bestSellMethod = false;

    public Position bestSellMethodPos = new Position(394, 142, false, true);

    @ConfigOption(name = "Cancelled Buy Order Clipboard", desc = "Saves missing items from cancelled buy orders to clipboard for faster re-entry.")
    @ConfigEditorBoolean
    public boolean cancelledBuyOrderClipboard = false;
}
