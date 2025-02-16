package at.hannibal2.skyhanni.config.features.misc;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.utils.ItemPriceSource;
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrackerConfig {

    @Expose
    @ConfigOption(name = "Hide with Item Value", desc = "Hide all trackers while the Estimated Item Value is visible.")
    @ConfigEditorBoolean
    public boolean hideInEstimatedItemValue = true;

    @Expose
    @ConfigOption(name = "Change Price Source", desc = "Change what price to use: Bazaar (Sell Offer or Buy Order) or NPC.")
    @ConfigEditorDropdown
    public ItemPriceSource priceSource = ItemPriceSource.BAZAAR_INSTANT_BUY;

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
        public boolean chat = true;

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
    @ConfigOption(name = "Show as Table", desc = "Show the list of items as a table.")
    @ConfigEditorBoolean
    public Property<Boolean> showTable = Property.of(true);

    @Expose
    @ConfigOption(name = "Items Shown", desc = "Change the number of item lines shown at once.")
    @ConfigEditorSlider(minValue = 3, maxValue = 30, minStep = 1)
    public Property<Integer> itemsShown = Property.of(10);

    @Expose
    @ConfigOption(name = "Hide outside Inventory", desc = "Hide Profit Trackers while not inside an inventory.")
    @ConfigEditorBoolean
    public boolean hideItemTrackersOutsideInventory = false;

    @Expose
    @ConfigOption(name = "Tracker Search", desc = "Add a search bar to tracker GUIs.")
    @ConfigEditorBoolean
    public Property<Boolean> trackerSearchEnabled = Property.of(true);

    @Expose
    @ConfigOption(
        name = "Text Order",
        desc = "Drag text to change the line format."
    )
    @ConfigEditorDraggableList
    public Property<List<TextPart>> textOrder = Property.of(new ArrayList<>(Arrays.asList(
        TextPart.AMOUNT,
        TextPart.NAME,
        TextPart.TOTAL_PRICE
    )));

    public enum TextPart {
        ICON("Item Icon"),
        NAME("Item Name"),
        AMOUNT("Amount"),
        TOTAL_PRICE("Total Price"),
        ;

        private final String displayName;

        TextPart(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
