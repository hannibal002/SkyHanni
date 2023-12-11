package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.HasLegacyId;
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class TrackerConfig {

    @Expose
    @ConfigOption(name = "Hide with Item Value", desc = "Hide all trackers while the Estimated Item Value is visible.")
    @ConfigEditorBoolean
    public boolean hideInEstimatedItemValue = true;

    @Expose
    @ConfigOption(name = "Show Price From", desc = "Show price from Bazaar or NPC.")
    @ConfigEditorDropdown()
    public PriceFromEntry priceFrom = PriceFromEntry.SELL_OFFER;

    public enum PriceFromEntry implements HasLegacyId {
        INSTANT_SELL("Instant Sell", 0),
        SELL_OFFER("Sell Offer", 1),
        NPC("NPC", 2);
        private final String str;
        private final int legacyId;

        PriceFromEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        PriceFromEntry(String str) {
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

    @Expose
    @ConfigOption(name = "Default Display Mode", desc = "Change the display mode that gets shown on default.")
    @ConfigEditorDropdown
    public Property<SkyHanniTracker.DefaultDisplayMode> defaultDisplayMode = Property.of(SkyHanniTracker.DefaultDisplayMode.REMEMBER_LAST);

    @Expose
    @ConfigOption(name = "Recent Drops", desc = "Highlight the amount in green on recently gained items.")
    @ConfigEditorBoolean
    public boolean showRecentDrops = true;

    @Expose
    @ConfigOption(name = "Exclude Hidden", desc = "Exclude hidden items in the total price calculation.")
    @ConfigEditorBoolean
    public boolean excludeHiddenItemsInPrice = false;
}
