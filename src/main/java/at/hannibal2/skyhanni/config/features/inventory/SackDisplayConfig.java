package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.HasLegacyId;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SackDisplayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show contained items inside a sack inventory.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

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
    @ConfigEditorDropdown()
    public NumberFormatEntry numberFormat = NumberFormatEntry.FORMATTED;

    public enum NumberFormatEntry implements HasLegacyId {
        DEFAULT("Default", 0),
        FORMATTED("Formatted", 1),
        UNFORMATTED("Unformatted", 2);

        private final String str;
        private final int legacyId;

        NumberFormatEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        NumberFormatEntry(String str) {
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
    @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 10,
        minStep = 1)
    public int extraSpace = 1;

    @Expose
    @ConfigOption(name = "Sorting Type", desc = "Sorting type of items in sack.")
    @ConfigEditorDropdown(values = {"Descending (Stored)", "Ascending (Stored)", "Descending (Price)", "Ascending (Price)"})
    public int sortingType = 0;

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
    @ConfigEditorDropdown()
    public PriceFormatEntry priceFormat = PriceFormatEntry.FORMATTED;

    public enum PriceFormatEntry implements HasLegacyId {
        FORMATTED("Formatted", 0),
        UNFORMATTED("Unformatted", 1);

        private final String str;
        private final int legacyId;

        PriceFormatEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        PriceFormatEntry(String str) {
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
    @ConfigOption(name = "Show Price From", desc = "Show price from Bazaar or NPC.")
    @ConfigEditorDropdown(values = {"Bazaar", "NPC"})
    public int priceFrom = 0;

    @Expose
    public Position position = new Position(144, 139, false, true);
}
