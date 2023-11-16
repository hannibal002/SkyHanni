package at.hannibal2.skyhanni.config.features.event.winter;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FrozenTreasureConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Tracks all of your drops from Frozen Treasure in the Glacial Caves.\n" +
            "§eIce calculations are an estimate but are relatively accurate."
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
            "§1§lFrozen Treasure Tracker",
            "§61,636 Treasures Mined",
            "§33.2m Total Ice",
            "§3342,192 Ice/hr",
            "§81,002 Compact Procs",
            " ",
            "§b182 §fWhite Gift",
            "§b94 §aGreen Gift",
            "§b17 §9§cRed Gift",
            "§b328 §fPacked Ice",
            "§b80 §aEnchanted Ice",
            "§b4 §9Enchanted Packed Ice",
            "§b182 §aIce Bait",
            "§b3 §aGlowy Chum Bait",
            "§b36 §5Glacial Fragment",
            "§b6 §fGlacial Talisman",
            " ",
        }
    )
    public List<Integer> textFormat = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 14, 15));

    @Expose
    @ConfigOption(name = "Only in Glacial Cave", desc = "Only shows the overlay while in the Glacial Cave.")
    @ConfigEditorBoolean
    public boolean onlyInCave = true;

    @Expose
    @ConfigOption(name = "Show as Drops", desc = "Multiplies the numbers on the display by the base drop. \n" +
        "E.g. 3 Ice Bait -> 48 Ice Bait")
    @ConfigEditorBoolean
    public boolean showAsDrops = false;

    @Expose
    @ConfigOption(name = "Hide Chat Messages", desc = "Hides the chat messages from Frozen Treasures.")
    @ConfigEditorBoolean
    public boolean hideMessages = false;

    @Expose
    public Position position = new Position(10, 80, false, true);
}
