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
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Compact Lines", desc = "Combine the §a+ §7and §c- §7lines into a single line.")
    @ConfigEditorBoolean
    public boolean compactLines = true;

    @Expose
    @ConfigOption(name = "Show Item Icon", desc = "Show the item icon next to the item name")
    @ConfigEditorBoolean
    public boolean showItemIcon = true;

    @Expose
    @ConfigOption(name = "Sacks", desc = "Also show items added and removed from stacks")
    @ConfigEditorBoolean
    public boolean sack = true;

    @Expose
    @ConfigOption(
        name = "Expire After",
        desc = "How long items show for after being picked up or dropped, in seconds."
    )
    @ConfigEditorSlider(minValue = 1, maxValue = 20, minStep = 1)
    public int expireAfter = 10;

    @Expose
    @ConfigLink(owner = ItemPickupLogConfig.class, field = "enabled")
    public Position pos = new Position(100, 100, false, true);
}


