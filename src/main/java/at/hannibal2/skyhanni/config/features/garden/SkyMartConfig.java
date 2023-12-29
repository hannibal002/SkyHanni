package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SkyMartConfig {
    @Expose
    @ConfigOption(name = "Copper Price", desc = "Show copper to coin prices inside the SkyMart inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean copperPrice = false;

    @Expose
    @ConfigOption(name = "Advanced Stats", desc = "Show the BIN price and copper price for every item.")
    @ConfigEditorBoolean
    public boolean copperPriceAdvancedStats = false;

    @Expose
    @ConfigOption(name = "Item Scale", desc = "Change the size of the items.")
    @ConfigEditorSlider(minValue = 0.3f, maxValue = 5, minStep = 0.1f)
    public double itemScale = 1.7;

    @Expose
    public Position copperPricePos = new Position(211, 132, false, true);
}
