package at.hannibal2.skyhanni.config.features.fishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
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
    @ConfigOption(name = "Show When Pickup", desc = "Show the fishing tracker for a couple of seconds after catching something even while moving.")
    @ConfigEditorBoolean
    public boolean showWhenPickup = true;
}
