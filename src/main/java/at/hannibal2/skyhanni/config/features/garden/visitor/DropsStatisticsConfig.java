package at.hannibal2.skyhanni.config.features.garden.visitor;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DropsStatisticsConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Tallies up statistic about visitors and the rewards you have received from them."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Text Format",
        desc = "Drag text to change the appearance of the overlay."
    )
    @ConfigEditorDraggableList(
        exampleText = {
            "§e§lVisitor Statistics",
            "§e1,636 Total",
            "§a1,172§f-§9382§f-§681§f-§c1",
            "§21,382 Accepted",
            "§c254 Denied",
            " ",
            "§c62,072 Copper",
            "§33.2m Farming EXP",
            "§647.2m Coins Spent",
            "§b23 §9Flowering Bouquet",
            "§b4 §9Overgrown Grass",
            "§b2 §5Green Bandana",
            "§b1 §9Dedication IV",
            "§b6 §b◆ Music Rune I",
            "§b1 §cSpace Helmet",
            "§b1 §9Cultivating I",
            "§b1 §9Replenish I",
            " ", // If they want another empty row
            "§212,600 Garden EXP",
            "§b4.2k Bits",
            "§220k Mithril Powder",
            "§d18k Gemstone Powder",
        }
    )
    public List<Integer> textFormat = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12));


    @Expose
    @ConfigOption(name = "Display Numbers First", desc = "Determines whether the number or drop name displays first. " +
        "§eNote: Will not update the preview above!")
    @ConfigEditorBoolean
    public boolean displayNumbersFirst = true;

    @Expose
    @ConfigOption(name = "Display Icons", desc = "Replaces the drop names with icons. " +
        "§eNote: Will not update the preview above!")
    @ConfigEditorBoolean
    public boolean displayIcons = false;

    @Expose
    @ConfigOption(name = "Only on Barn Plot", desc = "Only shows the overlay while on the Barn plot.")
    @ConfigEditorBoolean
    public boolean onlyOnBarn = true;

    @Expose
    public Position pos = new Position(5, 20, false, true);
}
