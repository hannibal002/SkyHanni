package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.HasLegacyId;
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
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

    @Expose
    @ConfigOption(name = "Item Warnings", desc = "Item Warnings")
    @Accordion
    public TrackerItemWarningsConfig warnings = new TrackerItemWarningsConfig();

    public static class TrackerItemWarningsConfig {

        @Expose
        @ConfigOption(name = "Price in Chat", desc = "Show an extra chat message when you pick up an expensive item. " +
            "(This contains name, amount and price)")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean chat = false;

        @Expose
        @ConfigOption(name = "Minimum Price", desc = "Items below this price will not show up in chat.")
        @ConfigEditorSlider(minValue = 1, maxValue = 20_000_000, minStep = 1)
        public int minimumChat = 5_000_000;

        @Expose
        @ConfigOption(name = "Title Warning", desc = "Show a title for expensive item pickups.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean title = true;

        @Expose
        @ConfigOption(name = "Title Price", desc = "Items above this price will show up as a title.")
        @ConfigEditorSlider(minValue = 1, maxValue = 50_000_000, minStep = 1)
        public int minimumTitle = 5_000_000;
    }

    @Expose
    @ConfigOption(name = "Hide Cheap Items", desc = "Hide cheap items.")
    @Accordion
    public HideCheapItemsConfig hideCheapItems = new HideCheapItemsConfig();

    public static class HideCheapItemsConfig {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Limit how many items should be shown.")
        @ConfigEditorBoolean
        public Property<Boolean> enabled = Property.of(true);

        @Expose
        @ConfigOption(name = "Show Expensive #", desc = "Always show the # most expensive items.")
        @ConfigEditorSlider(minValue = 1, maxValue = 40, minStep = 1)
        public Property<Integer> alwaysShowBest = Property.of(8);

        @Expose
        @ConfigOption(name = "Still Show Above", desc = "Always show items above this §6price in 1k §7even when not in the top # of items.")
        @ConfigEditorSlider(minValue = 5, maxValue = 500, minStep = 5)
        public Property<Integer> minPrice = Property.of(100);

    }
}
