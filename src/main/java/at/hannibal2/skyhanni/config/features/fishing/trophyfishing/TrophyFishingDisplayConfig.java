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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrophyFishingDisplayConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a display of all trophy fishes ever caught."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
    @ConfigEditorSlider(
        minValue = 0,
        maxValue = 10,
        minStep = 1)
    public int extraSpace = 1;

    @Expose
    @ConfigOption(name = "Sorted By", desc = "Sorting type of items in sack.")
    @ConfigEditorDropdown
    public TrophySorting sortingType = TrophySorting.ITEM_RARITY;

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
    public boolean reverseOrder = false;

    @Expose
    @ConfigOption(
        name = "Text Order",
        desc = "Drag text to change the line format."
    )
    @ConfigEditorDraggableList
    public List<TextPart> textOrder = new ArrayList<>(Arrays.asList(
        TextPart.NAME,
        TextPart.ICON,
//         TextPart.TOTAL
        TextPart.BRONZE,
        TextPart.SILVER,
        TextPart.GOLD,
        TextPart.DIAMOND
    ));

    public enum TextPart {
        NAME("Item Name"),
        ICON("Item Icon"),
        BRONZE("Amount Bronze"),
        SILVER("Amount Silver"),
        GOLD("Amount Gold"),
        DIAMOND("Amount Diamond"),
//         TOTAL("Amount Total"),
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
    @ConfigLink(owner = TrophyFishingDisplayConfig.class, field = "enabled")
    public Position position = new Position(144, 139, false, true);
}
