package at.hannibal2.skyhanni.config.features.fishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class FishingProfitTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Count all items you pick up while fishing.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    public Position position = new Position(20, 20, false, true);

    @Expose
    @ConfigOption(name = "Show Price From", desc = "Show price from Bazaar or NPC.")
    @ConfigEditorDropdown(values = {"Instant Sell", "Sell Offer", "NPC"})
    public int priceFrom = 1;

    @Expose
    @ConfigOption(name = "Recent Drops", desc = "Highlight the amount in green on recently caught items.")
    @ConfigEditorBoolean
    public boolean showRecentDropss = true;

    @Expose
    @ConfigOption(name = "Hide Moving", desc = "Hide the Fishing Profit Tracker while moving.")
    @ConfigEditorBoolean
    public boolean hideMoving = true;
}
