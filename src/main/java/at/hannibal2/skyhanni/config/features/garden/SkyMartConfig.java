package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import javax.swing.text.Position;

public class SkyMartConfig {
    @Expose
    @ConfigOption(name = "Copper Price", desc = "Show copper to coin prices inside the SkyMart inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean copperPrice = false;

    @Expose
    @ConfigOption(name = "Item Scale", desc = "Change the size of the items.")
    @ConfigEditorSlider(minValue = 0.3f, maxValue = 5, minStep = 0.1f)
    public double itemScale = 1;

    @Expose
    @ConfigLink(owner = SkyMartConfig.class, field = "copperPrice")
    public Position copperPricePos = new Position(211, 132, false, true);

    @Expose
    @ConfigOption(name = "Overlay Price", desc = "Toggle for Bazaar 'sell order' vs 'instant sell' price in copper price overlay.")
    @ConfigEditorDropdown
    public OverlayPriceTypeEntry overlayPriceType = OverlayPriceTypeEntry.INSTANT_SELL;

    public enum OverlayPriceTypeEntry {
        INSTANT_SELL("Instant Sell"),
        SELL_ORDER("Sell Order"),
        ;
        private final String str;


        OverlayPriceTypeEntry(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
