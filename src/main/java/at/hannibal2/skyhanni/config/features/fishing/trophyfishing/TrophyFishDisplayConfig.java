package at.hannibal2.skyhanni.config.features.fishing.trophyfishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrophyFishDisplayConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a display of all trophy fishes ever caught."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> enabled = Property.of(false);

    @Expose
    @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 10,
        minStep = 1)
    public Property<Integer> extraSpace = Property.of(1);

    @Expose
    @ConfigOption(name = "Sorted By", desc = "Sorting type of items in sack.")
    @ConfigEditorDropdown
    public Property<TrophySorting> sortingType = Property.of(TrophySorting.ITEM_RARITY);

    public enum TrophySorting {
        ITEM_RARITY("Item Rarity"),
        TOTAL_AMOUNT("Total Amount"),
        BRONZE_AMOUNT("Bronze Amount"),
        SILVER_AMOUNT("Silver Amount"),
        GOLD_AMOUNT("Gold Amount"),
        DIAMOND_AMOUNT("Diamond Amount"),
        HIGHEST_RARITY("Highest Rariy"),
        NAME("Name Alphabetical"),
        ;

        private final String str;

        TrophySorting(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(
        name = "Reverse Order",
        desc = "Reverse the sorting order."
    )
    @ConfigEditorBoolean
    public Property<Boolean> reverseOrder = Property.of(false);

    @Expose
    @ConfigOption(
        name = "Text Order",
        desc = "Drag text to change the line format."
    )
    @ConfigEditorDraggableList
    public Property<List<TextPart>> textOrder = Property.of(new ArrayList<>(Arrays.asList(
        TextPart.NAME,
        TextPart.ICON,
        TextPart.TOTAL,
        TextPart.BRONZE,
        TextPart.SILVER,
        TextPart.GOLD,
        TextPart.DIAMOND
    )));

    public enum TextPart {
        ICON("Item Icon"),
        NAME("Item Name"),
        BRONZE("Amount Bronze"),
        SILVER("Amount Silver"),
        GOLD("Amount Gold"),
        DIAMOND("Amount Diamond"),
        TOTAL("Amount Total"),
        ;

        private final String str;

        TextPart(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Only Show Missing", desc = "Only show Trophy Fishes that are still missing at this rarity.")
    @ConfigEditorDropdown
    public Property<HideCaught> onlyShowMissing = Property.of(HideCaught.NONE);

    public enum HideCaught {
        NONE("Show All"),
        BRONZE("Bronze"),
        SILVER("Silver"),
        GOLD("Gold"),
        DIAMOND("Diamond"),
        ;

        private final String str;

        HideCaught(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigLink(owner = TrophyFishDisplayConfig.class, field = "enabled")
    public Position position = new Position(144, 139, false, true);
}
