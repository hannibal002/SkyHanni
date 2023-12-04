package at.hannibal2.skyhanni.config.features.rift.motes;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class InventoryValueConfig {
    @Expose
    @ConfigOption(name = "Inventory Value", desc = "Show total Motes NPC price for the current opened inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Number Format Type", desc = "Short: 1.2M\n" +
        "Long: 1,200,000")
    @ConfigEditorDropdown(values = {"Short", "Long"})
    public int formatType = 0;

    @Expose
    public Position position = new Position(126, 156, false, true);
}
