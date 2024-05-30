package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ItemPickupLogConfig {

    @Expose
    @ConfigOption(name = "Item Pickup Log", desc = "Show a log of what items you pick up/drop and their amounts.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean on = true;

    @Expose
    @ConfigOption(
        name = "Expire After",
        desc = "How long items show for after being picked up or dropped, in seconds."
    )
    @ConfigEditorSlider(minValue = 1, maxValue = 20, minStep = 1)
    public int expireAfter = 5;

    @Expose
    @ConfigLink(owner = ItemPickupLogConfig.class, field = "on")
    public Position pos = new Position(100, 100, false, true);
}


