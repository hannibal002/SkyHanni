package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.HasLegacyId;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

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

    public enum OverlayPriceTypeEntry implements HasLegacyId {
        INSTANT_SELL("Instant Sell", 0),
        SELL_ORDER("Sell Order", 1),
        ;
        private final String str;
        private final int legacyId;

        OverlayPriceTypeEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        OverlayPriceTypeEntry(String str) {
            this(str, -1);
        }

        @Override
        public int getLegacyId() {
            return legacyId;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
