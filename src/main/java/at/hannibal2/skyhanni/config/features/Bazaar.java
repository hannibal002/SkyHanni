package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorButton;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class Bazaar {

    @Expose
    @ConfigOption(name = "Order Helper", desc = "Show visual hints inside the Bazaar Manage Order view when items are ready to pickup or outbid.")
    @ConfigEditorBoolean
    public boolean orderHelper = false;

    @Expose
    @ConfigOption(name = "Best Sell Method", desc = "Difference between sell instantly and sell offer.")
    @ConfigEditorBoolean
    public boolean bestSellMethod = false;

    @Expose
    @ConfigOption(name = "Best Sell Method Position", desc = "")
    @ConfigEditorButton(runnableId = "bestSellMethod", buttonText = "Edit")
    public Position bestSellMethodPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Cancelled Buy Order Clipboard", desc = "Saves missing items from cancelled buy orders to clipboard for faster re-entry.")
    @ConfigEditorBoolean
    public boolean cancelledBuyOrderClipboard = true;

    @Expose
    @ConfigOption(name = "Update Timer", desc = "A countdown timer for upcoming Bazzar data update.")
    @ConfigEditorBoolean
    public boolean updateTimer = false;

    @Expose
    @ConfigOption(name = "Update timer Position", desc = "")
    @ConfigEditorButton(runnableId = "bazzarUpdateTimer", buttonText = "Edit")
    public Position updateTimerPos = new Position(10, 10, false, true);
}
