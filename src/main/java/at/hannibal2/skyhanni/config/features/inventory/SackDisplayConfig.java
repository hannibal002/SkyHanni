package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.utils.ItemPriceSource;
import at.hannibal2.skyhanni.utils.RenderUtils;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SackDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show contained items inside a sack inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Highlight Full",
        desc = "Highlight items that are full in red.\n" +
            "§eDoes not need the option above to be enabled."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean highlightFull = true;

    @Expose
    @ConfigOption(name = "Number Format", desc = "Either show Default, Formatted or Unformatted numbers.\n" +
        "§eDefault: §72,240/2.2k\n" +
        "§eFormatted: §72.2k/2.2k\n" +
        "§eUnformatted: §72,240/2,200")
    @ConfigEditorDropdown
    public NumberFormatEntry numberFormat = NumberFormatEntry.FORMATTED;

    @Expose
    @ConfigOption(name = "Alignment", desc = "Change the alignment for numbers and money.")
    @ConfigEditorDropdown
    public RenderUtils.HorizontalAlignment alignment = RenderUtils.HorizontalAlignment.LEFT;

    public enum NumberFormatEntry {
        DEFAULT("Default"),
        FORMATTED("Formatted"),
        UNFORMATTED("Unformatted");

        private final String displayName;

        NumberFormatEntry(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Expose
    @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 10,
        minStep = 1)
    public int extraSpace = 1;

    @Expose
    @ConfigOption(name = "Sorting Type", desc = "Sorting type of items in sack.")
    @ConfigEditorDropdown
    public SortingTypeEntry sortingType = SortingTypeEntry.DESC_STORED;

    public enum SortingTypeEntry {
        DESC_STORED("Stored Descending"),
        ASC_STORED("Stored Ascending"),
        DESC_PRICE("Price Descending"),
        ASC_PRICE("Price Ascending");

        private final String displayName;

        SortingTypeEntry(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Expose
    @ConfigOption(name = "Item To Show", desc = "Choose how many items are displayed. (Some sacks have too many items to fit\n" +
        "in larger GUI scales, like the nether sack.)")
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 45,
        minStep = 1
    )
    public int itemToShow = 15;

    @Expose
    @ConfigOption(name = "Show Empty Item", desc = "Show empty item quantity in the display.")
    @ConfigEditorBoolean
    public boolean showEmpty = true;

    @Expose
    @ConfigOption(name = "Show Price", desc = "Show price for each item in sack.")
    @ConfigEditorBoolean
    public boolean showPrice = true;

    @Expose
    @ConfigOption(name = "Price Format", desc = "Format of the price displayed.\n" +
        "§eFormatted: §7(12k)\n" +
        "§eUnformatted: §7(12,421)")
    @ConfigEditorDropdown
    public PriceFormatEntry priceFormat = PriceFormatEntry.FORMATTED;

    public enum PriceFormatEntry {
        FORMATTED("Formatted"),
        UNFORMATTED("Unformatted");

        private final String displayName;

        PriceFormatEntry(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Expose
    @ConfigOption(name = "Change Price Source", desc = "Change what price to use: Bazaar (Sell Offer or Buy Order) or NPC.")
    @ConfigEditorDropdown
    public ItemPriceSource priceSource = ItemPriceSource.BAZAAR_INSTANT_BUY;

    @Expose
    @ConfigLink(owner = SackDisplayConfig.class, field = "enabled")
    public Position position = new Position(144, 139);
}
